using Newtonsoft.Json;

namespace AkkaInterop.AspNetWebApi.Models
{
	/// <summary>
	///		API model used to represent a greeting.
	/// </summary>
	public class GreetingModel
	{
		/// <summary>
		///		The name of the person being greeted.
		/// </summary>
		[JsonProperty("name")]
		public string Name { get; set; }

		/// <summary>
		///		The greeting for the person being greeted.
		/// </summary>
		[JsonProperty("greeting")]
		public string Greeting { get; set; }
	}
}
