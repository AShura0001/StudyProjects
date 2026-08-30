# SE325 Lab 05 - Concurrency & Web Sockets

In this exercise, we'll continue building out our Concert REST service. This time, we'll add one concurrency-related feature and one publish-subscribe feature using Web Sockets.

Since last time, our Concert service has been upgraded with additional functionality:

1. Concerts now have a `durationInMinutes` as well as a `startTime`.

2. `Concert`s now have a `standardTicketPrice` which must always be set, and a `specialTicketPrice` which organizers can use to place a concert "on special" (i.e. promo).

## Exercise One - Concert scheduling

### Part A - JPQL

Let's assume that all concerts in this service are scheduled in the same venue. This means that, if a concert is scheduled on a particular date / time, and has a particular duration, any newly-scheduled concert must **not** conflict in terms of its own date / time / duration.

There are two tests for this in `ConcertControllerIT` (`testCreateConcertWithOverlappingDateBefore()` and `testCreateConcertWithOverlappingDateAfter()`) - and both are currently failing. The problem is there is currently no checking for scheduling overlaps when adding new concerts.

To begin this exercise, add some kind of check to make sure a new concert doesn't overlap with existing concerts. **Hints:**

- Concerts c1 and c2 overlap if c1's start time is before c2's end time, _and_ c1's end time is after c2's start time.
- A concert's end time can be calculated from its start time + its duration
- The following is valid JPQL which can calculate a concert's end time from its start time plus duration: `function('timestampadd', MINUTE, c.durationInMinutes, c.startTime)`
- Operators such as `<`, `>` etc. can be used on `LocalDateTime`s inside JPQL

Using this information, come up with a JPQL query to help you detect overlapping concerts, add it to `ConcertRepository`, and use it in `ConcertController`'s `createConcert()` method to throw a `409 Conflict` error if an overlap is detected with the new concert.

Once complete, the two failing unit tests mentioned above should pass.

### Part B - Concurrency

If two concerts with conflicting dates are being added at the same time, they may both (incorrectly) be added to the database unless we have some kind of concurrency control mechanism.

In this case, because _any_ existing concert in the database could potentially conflict with a new concert to be added, there are only a few valid concurrency control options:

1. Lock the entire concerts table
2. Lock all rows in the concerts table
3. Ensure the "create concert" operation is a single atomic transaction, and is set to the _serializable_ isolation level.

Option 3 is probably the best (and also easiest) approach here. Investigate Spring's `@Transactional` annotation and how it can be used to both set a transactional boundary, and ensure a particular isolation level.

Note that there aren't really any tests for this part of the exercise (testing concurrency is notoriously difficult) - but at the very least, your solution here shouldn't cause any existing tests to fail!

## Exercise Two - Subscription specials

For this exercise, we will add the ability for organizers to assign "special" ticket prices for a concert, and for concertgoers to be notified when tickets go on special for a concert.

### Part A - REST method

Add a new endpoint for `PUT /concerts/{id}/special-ticket-price` to `ConcertController`. This should accept a single integer as JSON format in the request body, and set the `specialTicketPrice` of the concert with the given `id` to that value - or throw a `404` error if the concert doesn't extist.

Once complete, the test `testSetConcertSpecialPrice()` in `ConcertControllerIT` should pass.

### Part B - Subscribing

Next, add the WebSocket functionality to the app. Clients will need to be able to connect to `ws://<host>:<port>/ws` and then subscribe to `/topic/specials`, where they should be notified whenever your REST method above is called. The server should broadcast a `ConcertSpecialNotification` as JSON, which contains the id, title and special ticket price of the concert.

**Hints:**

- The `WebSocketConfig` configuration class can be "borrowed" from the `parolee-websockets` course exmaple with very little modification, if any.
- A `SimpMessagingTemplate` instance, which can be `@Autowired`, can be used in `ConcertController` to broadcast the notification to clients.
