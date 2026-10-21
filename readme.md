# LAVS: Loan Application and Verification System

SOFTENG 306, Project 2, Team 2.

LAVS is a Java desktop application that gives Countrywide Banking Corporation's Mobile Mortgage Managers (MMMs) a modern front end to the bank's legacy mainframe. The old text terminal is slow to learn, and a dropped connection means starting the whole loan application again. LAVS checks what the MMM types, saves it on the laptop first, and sends it to the mainframe when it can, so a lost connection never costs anyone their work.

The project specification is available [on Canvas](https://canvas.auckland.ac.nz/courses/142477/pages/project-2-overview?module_item_id=3007942). The four deliverables are:
* [Proposal](https://canvas.auckland.ac.nz/courses/142477/assignments/514280)
* [Presentation](https://canvas.auckland.ac.nz/courses/142477/assignments/514279)
* [Code](https://canvas.auckland.ac.nz/courses/142477/assignments/514277)
* [Documentation](https://canvas.auckland.ac.nz/courses/142477/assignments/514278)

## Team

* *Alastair - changecharacter*
* *Ivan - ivanyuch*
* *Ritvik - rsaj289*
* *Stephen - stephenwu100*
* *Thisas - ThisasG*

## What you need

* Java 17 or newer
* Maven 3.9 or newer (JavaFX and every other library is downloaded by Maven)
* Windows 10 or higher is the target. It also runs elsewhere, because it is all Java.

## Build and test

Everything is run from the project folder.

**Build**

```
mvn clean compile
```

**Run the tests**

```
mvn clean verify "-Dtest=lavs/**/*Test,!lavs/**/*IntegrationTest"
```

This runs every test that does not need the mock mainframe, and checks that the business layer has at least 95% line coverage (the build fails if it does not). Tests named `...IntegrationTest` and the provided tests under `src/test/java/mainframe` log in to the real mock server, so they only pass when the server is up and its data has not changed. Plain `mvn verify` runs those too.

## Running the application

The user interface is JavaFX 21 on Java 21 and starts from `lavs.ui.LavsApp`.

* `mvn javafx:run` starts the app at the login screen.
* In IntelliJ or VS Code, run `lavs.ui.Launcher` (a plain `main` that works on a normal classpath).
* Log in with the mainframe credentials below. The mainframe address is `https://animating-eraser-arrogance.ngrok-free.dev/`; override it with `-Dlavs.mainframe.url=...` or the `LAVS_MAINFRAME_URL` environment variable.
* To open a screen directly, pass its name: `mvn javafx:run -Djavafx.args=customerdetails`. Names: `login`, `loggedout`, `customersearch`, `searchresults`, `fulldirectory`, `customerdetails`, `loandetails`, `registercustomer`, `addloan`, `addcoborrower`, `editemployer`, `editpayment`, `editschedule`.
* Add `-Dlavs.stubs=true` to keep the customer and loan screens on built-in sample data (for example when the tunnel is down). Login still needs the mainframe.

The three dots beside Logout show the sync of saved changes: green when all sent, a white dot moving along while sending, two yellow when changes are waiting for the connection, one red when something needs checking (click it for details). The background sync starts when you log in and stops when you log out; drafts not yet sent are kept in `~/.lavs/drafts` and go at the next login. Preview a state with `-Dlavs.sync=sent|sending|waiting|attention`, which keeps the dots on that state even after login.

Keyboard shortcuts are listed on each screen with F1. Every shortcut is a second way to use a button that is already on screen.

The screens read customers and loans from the mainframe. Saving edits, creating loans and adding co-borrowers show a "not connected yet" message until the matching backend methods exist. The screens are laid out from the Figma mock-ups in the proposal; Register customer, Add loan and Add co-borrower have no mock-up and reuse the Customer Details card.

Bundled fonts (Jacquard 12, Red Hat Mono, NATS and Nunito) are open-licence (SIL OFL) fonts.

## Run the offline sync demo

The demo logs in to the real mock mainframe, saves a loan while the signal is switched off, closes and reopens the application, then brings the signal back and shows the loan arriving exactly once. It pauses between steps so you can talk through it.

```
mvn compile org.codehaus.mojo:exec-maven-plugin:3.1.0:java -Dexec.mainClass=lavs.demo.OfflineSyncDemo
```

It asks for the password of the mainframe user (see the credentials below). You can also run `lavs.demo.OfflineSyncDemo` from an editor. Options: `--url`, `--user`, `--password`, `--customer` and `--auto` (no pauses). The password can also be set in the `LAVS_PASSWORD` environment variable.

**The demo creates one real loan on the mainframe it connects to.** It keeps its drafts in a temporary folder and removes it at the end.

## How it is built

Three layers, and calls only go downward.

| Layer | Folder | What it does |
|---|---|---|
| Presentation | `src/main/java/lavs/ui` | JavaFX screens. No business rules. |
| Business logic | `src/main/java/lavs/business` | Every rule and calculation. It defines the interfaces the layers below it implement, so it can be tested with fakes and no mainframe. |
| Data access | `src/main/java/lavs/data`, `src/main/java/lavs/infrastructure` | Saving drafts to files, talking to the mainframe, and the background worker thread. |

`src/main/java/lavs/app/AppServices.java` creates the services and joins them in one place. The screens get it when the user logs in. `src/main/java/mainframe` is the provided mainframe client, which we use but do not change.

## How the offline sync works

This is the part of the design that answers the client's main complaint.

1. **Every record is saved to a file on the laptop first.** A customer, a loan and a co-borrower are each saved as a *draft*. The file is written to a temporary name and then renamed, so a crash part way through never leaves a half-written draft.
2. **A background worker sends the drafts to the mainframe**, one at a time, so the screen never waits for the network. A draft is only sent once the drafts it depends on have been accepted, so a loan goes after its customer.
3. **If the connection drops, nothing is lost.** The draft stays saved, and the worker tries again after a growing delay (about 1, 2, 4 seconds, up to a minute). After the application is closed and reopened, the saved drafts are picked up again.
4. **A record is never sent twice.** Before every attempt the draft is saved as "sending". If the reply is lost or the application stops part way through, the draft becomes "uncertain" and is not sent again until a person has checked the mainframe and said whether the record is there.
5. **If the mainframe refuses a record,** the draft is kept with the reasons, so the staff member can correct it and send it again.

The loan summary, the repayment schedule, changing an existing record and the relationships between customers are worked out or stored by the mainframe, so they need a live connection and say so clearly when there is none.

## Loan and relationship rules

* A loan needs a customer with a mailing address and a phone number or an email, and its values must pass the validation rules. Every problem is reported at once.
* A customer cannot be a co-borrower on their own loan.
* A customer cannot be related to themselves, and an "Other" relationship needs a description.

## Known limits

* Submitting a loan for checking (changing its status from New to Pending) is not part of the loan service. The adapter for it exists.
* A record whose outcome is uncertain waits for a person to check it. The services for that are in place (`BackgroundSync.findUncertain`, `confirmSynced` and `confirmNotSent`), but no screen calls them yet, so today the check is made by looking at the draft files in `~/.lavs/drafts`.
* A customer is sent to the mainframe as several messages (the customer, then each address, phone number, email and employer). If the connection drops part way, the customer draft is marked uncertain rather than sent again, so it is not repeated blindly.

## Mainframe credentials

Username: Group_2

Password: bug

## Provided code

This repository started with the code for the mainframe interface and its unit tests:

* [mainframe](src/main/java/mainframe/client/): the interfaces for sending requests to and receiving responses from the mainframe. [Connection](src/main/java/mainframe/client/Connection.java) is the interface our code uses, and [HttpConnection](src/main/java/mainframe/client/HttpConnection.java) connects to the web server.
* [tests](src/test/java/mainframe/client/): the unit tests. Most of them connect to the web server, and there is an [example](src/test/java/mainframe/client/SimulateListCustomers.java) of testing against a stub.

## Example mainframe access

    public static void main(String[] args) throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Connection httpConnection = new HttpConnection("https://animating-eraser-arrogance.ngrok-free.dev/");
        Status status = httpConnection.Login("admin", "admin");
        Search searchCustomers = new Search(1, null, 1);
        searchCustomers.send(httpConnection);
        Search.Item[] ItemList = searchCustomers.getItems();
        for (Search.Item i: ItemList){
            System.out.println(i);
        }

        Retrieve retrieveCustomer = new Retrieve("01-0001-0000020");
        retrieveCustomer.send(httpConnection);

        for (Field field : retrieveCustomer.getClass().getDeclaredFields()){
            String fieldName = field.getName();

            // Construct the getter method name (e.g., "getName" or "getAge")
            String getterName = "get" + Character.toUpperCase(fieldName.charAt(1)) + fieldName.substring(2);

            // Retrieve and invoke the method
            Method getter = retrieveCustomer.getClass().getMethod(getterName);
            Object value = getter.invoke(retrieveCustomer);

            System.out.println(fieldName + ": " + value);
        }
    }
