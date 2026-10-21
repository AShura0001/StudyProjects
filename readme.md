# Mainframe Interface: Project 2 Starter

Welcome to the second project for SOFTENG 306. 

The project specification is available [on Canvas](https://canvas.auckland.ac.nz/courses/142477/pages/project-2-overview?module_item_id=3007942). The four deliverables for this project are:
* [Proposal](https://canvas.auckland.ac.nz/courses/142477/assignments/514280)
* [Presentation](https://canvas.auckland.ac.nz/courses/142477/assignments/514279)
* [Code](https://canvas.auckland.ac.nz/courses/142477/assignments/514277)
* [Documentation](https://canvas.auckland.ac.nz/courses/142477/assignments/514278)

## Important

Update this readme file to contain the names of each team member and your GitHub login.

* *Alastair - changecharacter*
* *Ivan - ivanyuch*
* *Ritvik - rsaj289*
* *Stephen - stephenwu100*
* *Thisas - ThisasG*

You must also update this file to contain either the instructions for running your code or a link to the instructions.

## Running the application

The user interface is JavaFX 21 on Java 21 and starts from `lavs.ui.LavsApp`.

* `mvn javafx:run` starts the app at the login screen.
* In IntelliJ or VS Code, run `lavs.ui.Launcher` (a plain `main` that works on a normal classpath).
* Log in with the mainframe credentials below. The mainframe address is `https://animating-eraser-arrogance.ngrok-free.dev/`; override it with `-Dlavs.mainframe.url=...` or the `LAVS_MAINFRAME_URL` environment variable.
* To open a screen directly, pass its name: `mvn javafx:run -Djavafx.args=customerdetails`. Names: `login`, `loggedout`, `customersearch`, `searchresults`, `fulldirectory`, `customerdetails`, `loandetails`, `registercustomer`, `addloan`, `addcoborrower`, `editemployer`, `editpayment`, `editschedule`.

The three dots beside Logout show the sync of saved changes: green when all sent, a white dot moving along while sending, two yellow when changes are waiting for the connection, one red when something needs checking (click it for details). The background sync starts when you log in and stops when you log out; drafts not yet sent are kept in `~/.lavs/drafts` and go at the next login. Preview a state with `-Dlavs.sync=sent|sending|waiting|attention`, which keeps the dots on that state even after login.

Keyboard shortcuts are listed on each screen with F1. Every shortcut is a second way to use a button that is already on screen.

Every screen controller works through the business services (`CustomerService`, `LoanService`, `LoanAccountService`), which `AppContext` starts after login; there are no services in the `lavs.ui` package. New loans are saved as drafts and sent in the background. Customer details lists every address; addresses are added and edited on their own form and removed in edit mode. Not available yet: editing a single repayment (the mainframe works repayments out from the loan terms), photos, and changing notes. The screens are laid out from the Figma mock-ups in the proposal; Register customer, Add loan and Add co-borrower have no mock-up and reuse the Customer Details card.

Bundled fonts (Jacquard 12, Red Hat Mono, NATS and Nunito) are open-licence (SIL OFL) fonts.

## Initial Repository Code

This repository initially contains the code for the mainframe interface and the unit tests. The following code is supplied:
* [mainframe](src/main/java/mainframe/client/): the interfaces for sending requests to and receiving responses from the mainframe. You will need to use the [Connection](src/main/java/mainframe/client/Connection.java) interface in your code. We have provided an implementation that connects to a web server in [HttpConnection](src/main/java/mainframe/client/HttpConnection.java).
* [tests](src/test/java/mainframe/client/): the unit tests. Most of these connect to the web server, but there is an [example unit test](src/test/java/mainframe/client/SimulateListCustomers.java) for connecting to a stub.


## Mainframe credentials
Username: Group_2

Password: bug

## Example Mainframe access
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