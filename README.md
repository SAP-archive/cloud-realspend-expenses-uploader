# cloud-realspend-expenses-uploader

Allows posting budgets and expenses to HCP RealSpend API through SAP ERP JCO connection.

Prerequisites
-------------
- Java 1.6 or higher
- Maven
- sapjco3: https://websmp108.sap-ag.de/~sapidb/011000358700007415502002E/#2

Run Instructions
----------------
- Make sure you have installed all the prerequisites on your local machine.
- Clone the repository, and in the command-line change your current directory to it.
- Edit the file system.jcoDestination and insert the correct credentials for the SAP ERP system.
- Run the command shown in the **Build** section below. Now you see the generated "target" folder, which contains the executable jar file "expense-uploader-0.1-jar-with-dependencies.jar". Copy this jar and put it in a folder with all the required config files (see the notes section below). 
- You can either run the shell script run.sh now, or simply copy the command shown under the **Run** section below and run it on the command-line. Make sure you replace the command line parameters with the correct desired values.

Build
-----
```
mvn clean package
```

Run
---
- Run the jar like this:
```
java -cp ~/sapjco3/sapjco3.jar:expense-uploader-0.1-jar-with-dependencies.jar com.sap.expenseuploader.ExpenseUploader --from=20150301 --to=20150501 --controlling-area=0001 --input_erp=system --output_hcp="https://devx07e60597.neo.ondemand.com/core/basic/api/v1" --proxy=proxy.wdf.sap.corp:8080 --hcp_user=<your_hcp_username> --hcp_password=<your_hcp_password>  --with_budgets
```

Notes
-----
- All the config files of the script should be placed inside the created folder, just next to the jar file. This includes the input expenses excel sheets and the json configuration files.
- An example folder of this program should include minimally the following files: "budgets.json" - "costcenters.json" - "expense-uploader-0.1-jar-with-dependencies.jar" - "system.jcoDestination" 
- Sample files of all those input and configuration files are located under src/test/resources. Just copy and modify them.
- You need to execute the **Build** commands ideally only once you clone and setup the script. Later on you can just use the shell script or the command under the **Run** section
- You can place the "sapjco3" wherever suits you the best, but in the command in the **Run** section we assume it's located in your user home directory (under unix-based operating systems). Just replace its path in the command if it's located somewhere else.

Configuration files
-------------------
- There are two configuration files, budgets.json and costcenters.json. These are used to specify what data should be uploaded.
- In the file costcenters.json you must specify for each user, which cost centers are relevant. Only expenses related to these will be uploaded.
- In the file budgets.json you must specify the budgets for tags and master data. The following master data is available: account, accountnode, costcenter, costcenternode and internalorder
- For master data you specify the key (accounts use the account number, others use the node-name) and a list of budgets
- A budget has three attributes: amount, currency and year
- For tags you use the key "tags", followed by a map of tag-groups, then a map of tag-names and last a list of budgets

License
-------
See the [License](https://github.com/SAP/cloud-realspend-expenses-uploader/blob/master/License.md) file for license rights and limitations (ASL 2.0).
