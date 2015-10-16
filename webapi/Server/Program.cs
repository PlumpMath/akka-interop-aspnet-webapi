using Microsoft.Owin.Hosting;
using Microsoft.Owin.Hosting.Tracing;
using Owin;
using Serilog;
using System;
using System.IO;
using System.Threading;
using System.Web.Http;

namespace Server
{
	/// <summary>
	///		Self-hosted web API to be called from Akka HTTP.
	/// </summary>
	static class Program
	{
		/// <summary>
		///		The main program entry-point.
		/// </summary>
		static void Main()
		{
			int? port = GetListenPort();
			if (port == null)
				return;

			SynchronizationContext.SetSynchronizationContext(
				new SynchronizationContext()
			);

			ConfigureLogging();
			try
			{
				string listenUri = $"http://localhost:{port.Value}";
				Log.Information("Starting server on {ListenUri}...", listenUri);

				StartOptions hostOptions = BuildHostOptions(listenUri);
				using (WebApp.Start(hostOptions, ServerConfiguration))
				{
					Log.Information("Server started (press enter to terminate).");
					Console.ReadLine();
				}
			}
			catch (Exception unexpectedError)
			{
				Log.Error(unexpectedError, "Unexpected error: {ErrorMessage}", unexpectedError.Message);
			}
		}

		/// <summary>
		///		Build <see cref="StartOptions"/> to control the OWIN host's startup.
		/// </summary>
		/// <param name="listenUri">
		///		The URI on which the host will listen for incoming requests.
		/// </param>
		/// <returns>
		///		The <see cref="StartOptions"/>.
		/// </returns>
		static StartOptions BuildHostOptions(string listenUri)
		{
			if (String.IsNullOrWhiteSpace(listenUri))
				throw new ArgumentException("Argument cannot be null, empty, or entirely componsed of whitespace: 'listenUri'.", "listenUri");

			return new StartOptions(listenUri)
			{
				Settings =
					{
						// Disable OWIN tracing (maybe later we can replace it with Serilog).
						[typeof(ITraceOutputFactory).FullName] = typeof(NullTraceOutputFactory).AssemblyQualifiedName
					}
			};
		}

		/// <summary>
		///		Configure the web API and the OWIN pipeline that hosts it.
		/// </summary>
		/// <param name="app">
		///		The OWIN application builder.
		/// </param>
		static void ServerConfiguration(IAppBuilder app)
		{
			if (app == null)
				throw new ArgumentNullException("app");

			HttpConfiguration webApiConfiguration = new HttpConfiguration();
			webApiConfiguration
				.MapHttpAttributeRoutes();

			app.UseWebApi(webApiConfiguration);
		}

		/// <summary>
		///		Parse the HTTP listener port from command-line arguments.
		/// </summary>
		/// <returns>
		///		The port number, or <c>null</c> if no valid port was found in the command-line arguments.
		/// </returns>
		static int? GetListenPort()
		{
			string[] commandLineArguments = Environment.GetCommandLineArgs();

			int port;
			if (commandLineArguments.Length != 2 || !Int32.TryParse(commandLineArguments[1], out port))
			{
				Console.WriteLine(
					"Usage: {0} <HttpListenPort>",
					Path.GetFileNameWithoutExtension(
						commandLineArguments[0]
					)
				);

				return null;
			}

			return port;
		}

		/// <summary>
		///		Configure the global application logger.
		/// </summary>
		static void ConfigureLogging()
		{
			Log.Logger =
				new LoggerConfiguration()
					.MinimumLevel.Information()
					.Enrich.FromLogContext()
					.Enrich.WithThreadId()
					.WriteTo.Trace()
					.WriteTo.LiterateConsole(outputTemplate: "[{Level}] {Message}{NewLine}{Exception}")
					.CreateLogger();
		}

		/// <summary>
		///		Bloody OWIN.
		/// </summary>
		class NullTraceOutputFactory
			: ITraceOutputFactory
		{
			/// <summary>
			///		Create a tracewriter. Or don't, since I didn't ask you to.
			/// </summary>
			/// <param name="outputFile">
			///		Not used.
			/// </param>
			/// <returns>
			///		The <see cref="StreamWriter.Null"/> stream-writer.
			/// </returns>
			public TextWriter Create(string outputFile)
			{
				return StreamWriter.Null;
			}
		}
	}
}
