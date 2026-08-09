# SE325 Lab 01 - TCP, Sockets, and Serialization
In this lab, you'll get practical experience with content from Week One of SOFTENG 325.

## Exercise One - TCP and Serialization
To begin, take a look at [`Exercise One`](./lab-01-exercise-01). Here, you'll find a partially-completed application, consisting of a client and a server. The server hosts an employee database, while the client can add new employees to the database.

### Part A - Fixing the app
Running the app, you'll notice that the client and server can connect to each other just fine. However, there are errors when trying to add new employees using the client. Why is this? Correct the code such that this works correctly.

### Part B - Extending the app
Extend the app such that the client can additionally request a list of employees from the server. The server should respond by serializing its `employeeDatabase` list, and sending it back to the client. The client should deserialize and print out the list.

**Hint:** How might the server differentiate between a request to add a new employee, versus get the list of existing employees?

#### Important note: Object caching in `ObjectOutputStream`
The behaviour of `ObjectOutputStream` is such that, after serializing a particular object the first time, it will maintain a cache of its serialized form. If the stream is ever asked to serialize the same object again, it will send the cached value rather than actually serialize the object again. This may cause your code to behave unexpectedly. For example, consider the following:

```java
class Foo implements Serializable {
    private String bar;
    
    public String getBar() { return bar; }
    public void setBar(String bar) { this.bar = bar; }
}

class Main {
    void doStuff() {
        try (ObjectOutputStream out = new ObjectOutputStream(...)) {
            
            Foo myFoo = new Foo();
            myFoo.setBar("A");
            
            out.writeObject(myFoo);
            
            myFoo.setBar("B");
            out.writeObject(myFoo);
        }
    }
}
```

With that code, you would expect that a client reading the two `Foo` objects sent with the `ObjectOutputStream` would have their `bar` values set to `"A"` and `"B"`, respectively. However, that's not the case - the client would read two `Foo` objects, both of which would have `bar` set to `"A"`!

To overcome this, what we can do is, add a call out the output stream's `reset()` method, between the two calls to `writeObject()`. This will cause `ObjectOutputStream` to clear its cache, forcing it to properly serialize the same object again.

## Exercise Two - Commentary
In the box provided below, comment on the employee app, and more generally, Java apps built with TCP sockets and Java Serialization. In particular, consider the following:

- What steps are required to add new functionality?
- Is this application How easy (or difficult) do you think it would be to add another client which is _not_ written in java? Justify your answer.
- How does the server handle requests from multiple clients? How might this be improved?

```text
Your answer here.
```
