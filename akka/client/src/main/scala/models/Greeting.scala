package models

/**
 * Represents a greeting from ASP.NET web API.
 * @param name The name of the caller.
 * @param greeting A caller-specific greeting.
 */
case class Greeting(name: String, greeting: String)
