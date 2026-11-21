# SE325 Lab 03 - JSON / Jackson & Content negotiation

In this lab, we'll gain practice with the Jackson library to allow Plain Old Java Objects (POJOs) to be converted to and from the JSON format. We'll then explore SpringBoot's support for content negotiation and custom datatypes.

## Exercise One - Adding Jackson annotations to a Library system

This project contains a domain model for a simple system designed to allow users to borrow media from a library. The important domain model classes are:

- `User`: Represents a user of the system

- `Media`: Represents media which can be borrowed

- `Book`: One type of media

- `Film`: Another type of media

- `Library`: Maintains a user list, along with an inventory of available media

- `InventoryItem`: Represents a single entry in a library's inventory. Maps a particular media item to the number of copies of that item available for borrowing from a library.

In the test folder, we have a test file - `LibraryUnitTests`. The unit tests which exercise the functionality of the system seem to pass (examine those to determine what the functionality should be). However, the tests involving conversion to and from the JSON format currently fail.

The test failures can all be fixed by adding appropriate Jackson annotations at various points throughout the code, and making some other minor modifications.

To complete this lab exercise, make the unit tests pass, following these rules / guidelines:

- You may **not** edit the unit test class in any way, except for one line marked _TODO_ (line 43)

- You may **not** add or remove any *methods* from any class

- You *may* add extra constructors to various classes if desired

- You *may* add as many Jackson annotations as you like to any class / field / constructor / method

- You *may* add additional classes

### Hints
- Some properties in the classes have only getters, not setters. Consider how a [particular Jackson annotation](https://www.tutorialspoint.com/jackson_annotations/jackson_annotations_jsoncreator.htm) can be used to overcome this.

- Some external classes, i.e. `BufferedImage`, cannot be serialized / deserialized correctly by default. What Jackson feature will you use to support these properties? You may borrow classes from `se325-examples` rather than writing them yourself, if desired.

- How will you deal with the polymorphism which exists in this system?

- How can you ensure that there will only be one copy of each `Book` and `Film` instance in the deserialized libary (a constraint which already exists in the library before it is serialized)?

## Exercise Two - Pagination (HATEOAS)

In this exercise, we'll extend the Library REST service (provided by `InventoryController` and `UserController`) with some pagination support.

To begin, make sure Exercise One is complete. Once it is, all of the `UserControllerIT` and `InventoryControllerIT` integration tests should pass.

For this exercise, modify the `getAllInventoryItems()` and `getAvailableInventoryItems()` methods in `InventoryController` so that they support pagination. Specifically:

- An optional `page` parameter (`Integer`, default is `null`) can be supplied. If supplied, this enables pagination.

- If pagination is enabled, we will also want a `size` parameter (`int`, default value `5`) which will determine how many items per page.

- If pagination is enabled, only return the results on that "page". For example, if `page` is `1` and `size` is `5`, then the first five items will be returned. If `page` is `2`, the second five items will be returned, and so on.

- In addition to the returned items, add headers to the response (will need to change the return type to `ResponseEntity`)
  - If the `page` is > `1`, return a `Prev-Page` header whose value is the number of the previous page.
  - If there are more results after the current page, return a `Next-Page` header whose value is the number of the next page.

- If `page` is too high, an empty array with no headers (but still `200` response code) should be returned.

- If `page` is too low (i.e. <= `0`), a `400` bad request error should be returned.

Once complete, the integration tests in `InventoryControllerPaginationIT` should pass.

## Exercise Three - Content negotiation

In this exercise, modify the REST endpoints at `GET /inventory` and `GET /inventory/available` to produce _either_ `application/json` (as they currently are), or `text/csv`, depending on the `Accept` header given by the client.

When `application/json` data is requested, the endpoints should function as-is. When `text/csv` data is requested, the output should be similar to the following:

```txt
"BarCode","Type","Title","Count"
"000000001","Book""Twenty Thousand Leagues Under the Sea",3
"000000002","Film","Black Widow",1
```

Note the CSV header row.

Regardless of whether JSON or CSV data is requested by the client, the pagination logic (including HTTP headers) implemented in Exercise Two should still work.

Once complete, the tests related to CSV files in `InventoryControllerContentNegotiationIT` should pass.

## Exercise Four - Custom content negotiation

In the final exercise, add a new endpoint for `GET /inventory/{barCode}/cover-image`. This endpoint should produce `image/png` data, and should have the following behaviour:

- If the inventory item with the given bar code is not found, return `404`.

- If the inventory item with the given bar code is found, but its `coverImage` is `null`, return `404`.

- Otherwise, return the `BufferedImage` in `image/png` format.

You should have your REST endpoint method in `InventoryController` return a `BufferedImage` instance, and have SpringBoot convert to the correct binary format by extending `AbstractHttpMessageConverter`, and configuring SpringBoot with an instance of your new class.

Once complete, the tests related to images in `InventoryControllerContentNegotiationIT` should pass.