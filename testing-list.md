# Expense Uploader TESTS

EXPENSES:
---------
- test whether correct users receive correct expenses based on the assigned cost centers
    * done

- test whether date range is correctly applied during expense import

- test invalid date range

- test behavior if ERP is not reachable (wrong config)
    * done 

- test if merging of expenses works correctly (double import etc.)
    * implemented with using resume functionality 

- test whether BAPI import and export to Excel via tool works with large data and invalid data
    * done

- test whether BAPI import and export to HCP via tool works with large data and invalid data
    * done

- ensure to refetch CSRF token
    * it's now fetched for each post/put request

- TODO get IOO user that is allowed to call the BAPI
    * done by Sander

- test missing mandatory properties of line items in expense import (E.g. missing date) -> error code?
	* If expenses from ERP, this will not happen. E.g. the date, expense type and amount are filled automatically.
	* If input from Excel sheet:
		- If date is missing: now it prints the following and stops the program:
		    * Field 'Item Date' is mandatory, please insert the date for all line items in the format yyyy-MM-dd. E.g. 2015-04-29
		- If amount is missing: now it prints the following and stops the program:
            * Line item 'Amount' field is mandatory. Please enter the amounts for all expenses as numbers. E.g. 10.54
		- If type is missing: now it prints the following and stops the program:
		    * Field 'Cost Type' is mandatory, please insert the type for all line items.
		- If the currency is missing: now it prints the following and stops the program:
		    * Field 'Currency' is mandatory, please insert the currency for all line items.

- test invalid values of mandatory and optional properties of line items (e.g. date like 9999-99-99 or empty string etc.) -> error code?
	* for invalid date (9999-99-99):
		- Now the tool expense uploader has a small validation for the date and it prints:
            * The inserted date 9999-99-99 is unparseable. Please enter a valid date in the correct date format yyyy-MM-dd. E.g. 2015-04-29
	* for invalid currency "EUROOO":
		- there was no error received from realspend. this is fine?
	* for invalid amount "878sdfdf":
		- the tool expense uploader validates it and prints the message:
		    * Line item 'Amount' field is mandatory. Please enter the amounts for all expenses as numbers. E.g. 10.54
	* for invalid type "sddf":
		- the reponse from realspend was:
		    * status code: 400
		    * body:
		        ```Error is: {"error":"invalid_parameter"}```
	* for account "jjj":
		- no error message from realspend. this is fine?

- consider to remove approver (mandatory field?), should be left empty
	* I tested removing it from the payload now and no failure happened. It can be removed I assume.
	* But it's not removed now, we still put it and we set the value to be the currently logged in governance user (the guy who uses the tool).

- test behavior with large amount of data from ERP (try IOO, might have to create new user or get privileges), might cause issues with batch import on HCP database side (e.g., starting with 10000 line items, increase by factor 10)
	* done by Benny

- test whether Excel import via tool works with large data and invalid data
	* done by Benny

BUDGETS:
--------

- test overwrite of budget
    * done

- tool should create missing tags (test missing tags or master data)
	* done

- test put of budget on valid tags / master data to ensure users receive correct budgets
	* not working for tags, received the following:
	    - Error is: {"error":"unexpected_error"}
	* works for master-data (tested with cost-center)

- test put of budget on missing tags / master data to ensure users receive correct budgets
	* for missing tags -> they're created when they're missing
	* for master-data (tested with cost-center):
		- Status code: 500
		- URL was: https://devx07e60597.neo.ondemand.com/core/basic/api/v1/rest/budget/cost-center
		- Payload was:
            ```{"user":"d065350","budgets":[{"name":"1","amount":200.0,"currency":"EUR","year":2016},{"name":"1","amount":12345.0,"currency":"EUR","year":2015}]}```
		- Response Body:
            ```Error is: {"error":"unexpected_error"}```
