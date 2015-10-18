using AkkaInterop.AspNetWebApi.Models;
using System.Web.Http;
using System.Web.Http.Description;

namespace AkkaInterop.AspNetWebApi.Server.Controllers
{
	/// <summary>
	///		Controller for the greeting API.
	/// </summary>
	[RoutePrefix("api/v1/greet")]
	public class GreetingController
		: ApiController
	{
		/// <summary>
		///		Greet the caller by name.
		/// </summary>
		/// <param name="name">	
		///		The caller's name (from the query string).
		/// </param>
		/// <returns>
		///		Ok, 
		/// </returns>
		[HttpGet, Route("me")]
		[ResponseType(typeof(GreetingModel))]
		public IHttpActionResult GreetMeByName(string name = null)
		{
			if (string.IsNullOrWhiteSpace(name))
			{
				ModelState.AddModelError("name", "What's your name?");

				return BadRequest(ModelState);
			}

			return Ok(
				new GreetingModel
				{
					Name = name,
					Greeting = $"Hello {name} (greetings from ASP.NET WebAPI)."
				}
			);
		}
	}
}
