# Expense Uploader TODOs

EXPENSES:
---------
- test whether correct users receive correct expenses based on the assigned cost centers

- test whether date range is correctly applied during expense import

- test invalid date range

- test behavior if ERP is not reachable (wrong config)

- test if merging of expenses works correctly (double import etc.)

- test whether BAPI import and export to Excel via tool works with large data and invalid data

- test whether BAPI import and export to HCP via tool works with large data and invalid data

- ensure to refetch CSRF token

- TODO get IOO user that is allowed to call the BAPI

<span style='color:green'>

- test missing mandatory properties of line items in expense import (E.g. missing date) -> error code?
	* If expenses from ERP, this will not happen. E.g. the date, expense type and amount are filled automatically.
	* If input from Excel sheet:
		- If date is missing: now it prints the following and stops the program:
		&nbsp;Field 'Item Date' is mandatory, please insert the date for all line items in the format yyyy-MM-dd. E.g. 2015-04-29
		- If amount is missing: now it prints the following and stops the program:
        &nbsp;Line item 'Amount' field is mandatory. Please enter the amounts for all expenses as numbers. E.g. 10.54
		- If type is missing: now it prints the following and stops the program:
		&nbsp;Field 'Cost Type' is mandatory, please insert the type for all line items.
		- If the currency is missing: now it prints the following and stops the program:
		&nbsp;Field 'Currency' is mandatory, please insert the currency for all line items.

- test invalid values of mandatory and optional properties of line items (e.g. date like 9999-99-99 or empty string etc.) -> error code?
	* for invalid date (9999-99-99):
		- the response received from Realspend is:
			&nbsp;<span style='color:red'>status code: 500
			&nbsp;body:
			&nbsp;&nbsp;Error is: {"error":"unexpected_error"}</span>
		- Now the tool expense uploader has a small validation for the date and it prints:
		    &nbsp;The inserted date 9999-99-99 is unparseable. Please enter a valid date in the correct date format yyyy-MM-dd. E.g. 2015-04-29
	* for invalid currency "EUROOO":
		- there was no error received from realspend. this is fine?
	* for invalid amount "878sdfdf":
		- the tool expense uploader validates it and prints the message:
		&nbsp;Line item 'Amount' field is mandatory. Please enter the amounts for all expenses as numbers. E.g. 10.54
	* for invalid type "sddf":
		- the reponse from realspend was:
		&nbsp;<span style='color:red'>status code: 400
		&nbsp;body:
			&nbsp;Error is: {"error":"invalid_parameter"}</span>
	* for account "jjj":
		- no error message from realspend. this is fine?

- consider to remove approver (mandatory field?), should be left empty
	* I tested removing it from the payload now and no failure happened. It can be removed I assume.
	* But it's not removed now, we still put it and we set the value to be the currently logged in governance user (the guy who uses the tool).

- test behavior with large amount of data from ERP (try IOO, might have to create new user or get privileges), might cause issues with batch import on HCP database side (e.g., starting with 10000 line items, increase by factor 10)
	* done by Benny

- test whether Excel import via tool works with large data and invalid data
	* done by Benny
</span>

BUDGETS:
--------

- test overwrite of budget


<span style='color:green'>

- tool should create missing tags (test missing tags or master data)
	* done

- test put of budget on valid tags / master data to ensure users receive correct budgets
	* not working for tags, received the following:
	&nbsp;<span style='color:red'>Error is: {"error":"unexpected_error"}</span>
	* works for master-data (tested with cost-center)

- test put of budget on missing tags / master data to ensure users receive correct budgets
	* for missing tags -> they're created when they're missing
	* for master-data (tested with cost-center):
		<span style='color:red'>Status code:
		&nbsp;500
		URL was:
		&nbsp;https://devx07e60597.neo.ondemand.com/core/basic/api/v1/rest/budget/cost-center
		Payload was:
        &nbsp;{"user":"d065350","budgets":[{"name":"1","amount":200.0,"currency":"EUR","year":2016},{"name":"1","amount":12345.0,"currency":"EUR","year":2015}]}
		Response Body:
        &nbsp;Error is: {"error":"unexpected_error"}</span>
        
</span>
