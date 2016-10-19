# cloud-realspend-expenses-uploader

Allows posting budgets and expenses to HCP RealSpend API through SAP ERP JCO connection.

Prerequisites
-------------
- Java 1.6 or higher
- Maven 3
- sapjco3: https://websmp108.sap-ag.de/~sapidb/011000358700007415502002E/#2

Build
-----
- Clone the repository.
- Edit the file system.jcoDestination and insert the correct credentials for the SAP ERP system.
- Edit the pom.xml file and insert the correct path to JCO.
- Run the following command:
```
mvn clean package -DskipTests
```
- Now you see the generated "target" folder, which contains the executable jar file "expense-uploader-0.1-jar-with-dependencies.jar".
- Create a new folder somewhere (e.g. your home directory) and copy this jar into it. Also copy the files system.jcoDestination and src/test/resources/config/config.xlsx there. You should now have a folder with just these three files.

Run
---
- Open a command line prompt in the folder you just created.
- The hcp_proxy parameter is optional.
- The hcp_user must have the roles "Governance", "Manager" and "API-User". It is advised to have only one user with the Governance role, as multiple may lead to conflicts.
- Run the jar like this (after replacing the parts inside angled brackets):
```
java -cp <path-to-jco>/sapjco3.jar:expense-uploader-0.1-jar-with-dependencies.jar com.sap.expenseuploader.ExpenseUploader --from=<starting-date> --to=<end-date> --controlling-area=<ca-code> --input_erp=system --output_hcp --hcp_url="https://<Realspend-HCP-URL>/core/basic/api/v1" --hcp_user=<your_hcp_username> --hcp_proxy=proxy.wdf.sap.corp:8080 --budgets
```

Notes
-----
- You can find the Realspend-HCP-URL by navigating to the "RealSpend Launchpad" inside your HCP trial account. It's the part before "/core/".
- You can place the "sapjco3" wherever suits you the best.

Configuration files
-------------------
- The configuration is done using the Excel file "config.xlsx".
- On the first sheet you specify which user will receive actuals for which cost center.
- On the subsequent sheets you specify budgets for master data and tags.

License
-------
See the [License](https://github.com/SAP/cloud-realspend-expenses-uploader/blob/master/License.md) file for license rights and limitations (ASL 2.0).
