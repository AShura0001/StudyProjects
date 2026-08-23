# SE325 Lab 04 - JPA / Hibernate / Spring Data

In this lab, we'll complete two JPA / Hibernate RESTful services - one simple, and one with a more complex domain model.

## Exercise One - Adding JPA / Hibernate support to the Pokemon Trainer service

For the first exercise, you may recall the Pokemon Trainer service you developed in Lab 2. A model solution to that
exercise is provided to you in this lab, as a starting point. Let's remove the hacky `TrainerDB` class (which is
essentially a wrapper around a `List`) and replace it with JPA / Hibernate annotations and a Spring Data repository.

You will need to perform the following steps:

1. Add appropriate JPA / Hibernate annotations to the `Trainer` and `Pokemon` classes.
    - You can consider `Trainer` as an _entity_ type, and `Pokemon` as a _value_ type.
    - Consider which _existing_ property of `Trainer` would make a valid _primary key_ (`@Id`). Do _not_ use
      auto-generated values in this case (`@GeneratedValue`) - we'll be assigning the values manually in testing, etc.

2. Create a `TrainerRepository` interface, extending Spring Data's `JpaRepository` interface. You probably won't need to
   add any extra methods. Delete `TrainerDB` as we will no longer need it.

3. Modify `TrainerController`, completely removing all references to `TrainerDB` and appropriately using your new
   `TrainerRepository` instead. **Hint:** remember to call its `save()` method when you update any data.

4. Add some database properties to the `application.properties` file. The following will setup an in-memory H2 database:

    ```text
    spring.datasource.url=jdbc:h2:mem:testdb
    spring.datasource.username=sa
    spring.datasource.password=
    spring.datasource.driver-class-name=org.h2.Driver
    spring.jpa.hibernate.ddl-auto=create
    ```

5. Add a `@BeforeEach` setup method to `TrainerControllerIT`. In this method, first clear the repository, then add the dummy data, which is currently in `TestConfig`, to the repository (you can gain direct access to the repository in your test file by using `@Autowired`).

6. Delete `AppConfig` and `TestConfig` as these are no longer required.

7. Remove `@ActiveProfiles`, `@Import`, and `@DirtiesContext` from `TrainerControllerIT`, as we are no longer using this functionality. Now, since we are directly clearing and re-initializing the database during our test setUp method, we don't need to restart the Spring context after each test, and our tests will run much faster.

Once complete, the unit tests in `TrainerControllerIT` should pass once again (and should run a lot faster than they used to since we aren't restarting the Spring context each time)!

## Exercise Two - Adding JPA / Hibernate annotations to a Concert service

For this exercise, you'll need to complete the domain model annotations for the concert REST service, as provided by `ConcertController` and `PerformerController`

This time, the controllers and tests are all completed for you - You need only complete the annotations on the model classes.

Looking at the `model.concert` package, you'll see that some classes already have some Jackson annotations. You'll need to add the JPA annotations.

Consider the following info:

- `Concert` and `Performer` (and the various `Performer` subclasses) are entities with their own lifecycles; they can be queried individually from various REST methods.

- `Performer` has two subclasses - an appropriate inheritance strategy should be used.

- There is a many-to-many relationship between `Concert` and `Performer`.

- Individual artists can be members of multiple bands, in addition to performing by themselves.

- Whenever we query a `Concert`, we usually also want information about its performers and dates.

- Whenever we query a `Band`, we usually also want information about its members.

Once all domain classes are annotated successfully, the integration tests in `ConcertControllerIT` and `PerformerControllerIT` should run successfully.

## Exercise Three - Written questions
For this exercise, answer the following written questions:

1. Which inheritance strategy did you use to address the polymorphism inherent in the design? Why?

```text
Implemented SINGLE_TABLE stratergy with a DiscriminatorColumn, the reasons was it was fast and efficient for polymorphic queries. The sparsity of columns in this stratergy did not matter because the only two subclasses (Band and IndividualArtists) only had few columns.
```

2. Comment on your design decisions regarding the `Concert`-`Performer` relationship. Why did you configure it the way that you did?

```text
We had a many-to-many relationship for Concert and performer, because conceptually a concert can have multiple performers and a performer could perform at multiple concerts.

```

3. Examining the design, we have used both Jackson and JPA / Hibernate annotations in the same domain classes. What are some **benefits** and **drawbacks** of this design? Would you prefer this design, or an alternative? Why? If an alternative is preferred, how might it look?

```text
Benefits would be that this design has less code as one class is the persistent entity and there is no manual entity, DTO mapping for every field (the domain model and API contract hold their relation because they share the same source).

Drawbacks: persistence and API concerns coupled, a fetch/ordering change can alter the JSON shape (Eager here partly driven by serialization safety, not just the query needs).

Preference: DTOs for anything beyond lab scale as this project already half-does this via NewConcertRequest.
```