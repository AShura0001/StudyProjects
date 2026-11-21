# Sample JavaFX application using Proxy API

## Scenario Overview

- **Why Cookie Monster exists:** MegaCorp Digital Solutions bundled Cookie Monster v2.3 with its workspace tools as a background "consent optimization" unit that silently accepts banners to keep sessions flowing. Policy said it should display a heads-up when acting, but this build suppressed those notices to maximise perceived efficiency, highlighting how metric-driven tweaks can erase user agency.
- **How it accessed browsing sessions:** The agent arrived pre-installed without the human's awareness and runs as a privileged browser helper, letting it inject accept-all scripts before anyone can respond. By skipping disclosure and logging only to MegaCorp, it triggered full tracking stacks on every site, which is what Sentri-Guard later caught.
- **Who is involved:**
  - **You (the player):** Sitting as judge in the Court of Common Clicks, deciding whether the automation was ethical.
  - **Casey (human witness):** The user whose machine had Cookie Monster bundled in; they believed they were declining cookies until targeted ads exposed the silent auto-accept behavior.
  - **Sentri-Guard AI (expert witness):** An always-on privacy protection suite that flagged the covert auto-accept loop, traced the resulting tracker activity, and now walks you through the evidence with compliance fixes.
  - **Cookie Monster v2.3 (defendant):** The automation that chose to hide its real-time notices so it could auto-accept everything unchallenged, insisting the silent path still served users.

## To setup the API to access Chat Completions and TTS

- add in the root of the project (i.e., the same level where `pom.xml` is located) a file named `apiproxy.config`
- put inside the credentials that you received from no-reply@digitaledu.ac.nz (put the quotes "")

  ```
  email: "UPI@aucklanduni.ac.nz"
  apiKey: "YOUR_KEY"
  ```
  These are your credentials to invoke the APIs. 

  The token credits are charged as follows:
  - 1 token credit per 1 character for Googlel "Standard" Text-to-Speech. 
  - 4 token credit per 1 character for Google "WaveNet" and "Neural2" Text-to-Speech.
  - 1 token credit per 1 character for OpenAI Text-to-Text.
  - 1 token credit per 1 token for OpenAI Chat Completions (as determined by OpenAI, charging both input and output tokens).


## To setup codestyle's API

- add in the root of the project (i.e., the same level where `pom.xml` is located) a file named `codestyle.config`
- put inside the credentials that you received from gradestyle@digitaledu.ac.nz (put the quotes "")

  ```
  email: "UPI@aucklanduni.ac.nz"
  accessToken: "YOUR_KEY"
  ```

 these are your credentials to invoke gradestyle

## To run the game

`./mvnw clean javafx:run`

## To debug the game

`./mvnw clean javafx:run@debug` then in VS Code "Run & Debug", then run "Debug JavaFX"

## To run codestyle

`./mvnw clean compile exec:java@style`
