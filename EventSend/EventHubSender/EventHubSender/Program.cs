using System;
using System.Text;
using System.Threading.Tasks;
using Azure.Messaging.EventHubs;
using Azure.Messaging.EventHubs.Producer;


namespace EventHubSender
{

    class Program
    {
        // connection string to the Event Hubs namespace
        private const string connectionString = "Fill Me in"

        // name of the event hub
        private const string eventHubName = "crm";

        // number of events to be sent to the event hub
        private const int numOfEvents = 7000;

        // The Event Hubs client types are safe to cache and use as a singleton for the lifetime
        // of the application, which is best practice when events are being published or read regularly.
        static EventHubProducerClient producerClient;

        static async Task Main()
        {
            // Create a producer client that you can use to send events to an event hub
            producerClient = new EventHubProducerClient(connectionString, eventHubName);

            // Create a batch of events 
            using EventDataBatch eventBatch = await producerClient.CreateBatchAsync();

            var generator = new RandomGenerator();

            for (int i = 1; i <= numOfEvents; i++)
            {
                var randomNumber = generator.RandomNumber(5, 100);
                Console.WriteLine($"Random number between 5 and 100 is {randomNumber}");

                var randomString = generator.RandomString(10);
                Console.WriteLine($"Random string of 10 chars is {randomString}");

                var randomPassword = generator.RandomPassword();
                Console.WriteLine($"Random string of 6 chars is {randomPassword}");

                var obj = new
                {
                    files = new[]
                    {
                        new
                        {
                            sequence = i,
                            value    = randomNumber,
                            name     = randomString,
                            password = randomPassword
                        }
                    }
                };
                var jsonObject = System.Text.Json.JsonSerializer.Serialize(obj);

                if (!eventBatch.TryAdd(new EventData(Encoding.UTF8.GetBytes(jsonObject))))
                {
                    // if it is too large for the batch
                    throw new Exception($"Event {i} is too large for the batch and cannot be sent.");
                }
             
            }

            try
            {
                // Use the producer client to send the batch of events to the event hub
                await producerClient.SendAsync(eventBatch);
                Console.WriteLine($"A batch of {numOfEvents} events has been published.");
            }
            finally
            {
                await producerClient.DisposeAsync();
            }
        }
    }
}
