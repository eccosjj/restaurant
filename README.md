### Environment
	# Java version: 11.0.9
	# Apache Maven 3.6.3

### Application configurations (under src/main/resources)
	# config.json
		{
		    "order": {
		        "one_time_receive": 2, // define how many orders the kitchen receives.
		        "receive_interval": 1  // define time interval between the current receive and the next receive.
		    },
		    "courier": {
		        "wait_min": 2, // the minimum second waiting before the courier pick up the order.
		        "wait_max": 6  // the maximum second waiting before the courier pick up the order.
		    },
		    "moving_order_time_out": 10 // the time out configuration when the choosing an order from overflow when it's full.
		}

	# shelves.json
		{
		    "hot": {
		        "name": "Hot shelf", // shelf name
		        "allowableTemperature": "hot", // shelf allowed temperature
		        "capacity": 10, // shelf capacity
		        "shelfDecayModifier": 1 // decay modifier
		    },
		    ...
		    ...
		    "anyTemperature": {
		        "name": "Overflow shelf",
		        "allowableTemperature": "anyTemperature",
		        "capacity": 15,
		        "shelfDecayModifier": 2,
		        "isOverFlowShelf": true // mark this shelf is the overflow shelf.
		    }
		}
		
	# orders.json // just download file, no modifications.

### How to execute application
	# Option 1
		# cd restaurant
		# mvn assembly:assembly -Dmaven.test.skip=true
		# java -jar target/restaurant-0.0.1-SNAPSHOT-jar-with-dependencies.jar

	Option 2
		# import the application to eclipse or idea
		# right click the Restaurant.java under the package restaurant, and select Run as Java application.

### Application output
	# console: 
		Will display the list of options when the overflow shelf is full, and then print the moving process after the user choose an order by typing in the No#.
	
	# log: Every time the application is running, a folder named as the current million second generated under the log directory.
		# orders.log
			Print a snapshot of the delivery list, discard list and the full shelves’ contents. it occoured when any order is received, put into the shelf, moved from overflow shelf to other shelf, discarded or delivered.(the shelves’ content is not consistence to the event message because other thread is still modifying the shelves’ content while printing the snapshot)
		# process.log
			Print all orders's events, there are more detail events which doesn't trigger the snapshot to void harm the performance. (example: The kitchen 173 is failed to put the order:a0023411-c19d-4723-b6d1-6b8c8ff1b417 into Frozen shelf, now try to put it into Overflow shelf).
		# moveOrder.log
			Store the events of moving order from the overflow to other shelf when it's full, basicaly same as the console print.

### A description of how and why chosing to handle moving orders to and from the overflow shelf
	
	# Key point: The shelf should be locked in any put or take action, which means only 1 thread could modify the shelf at the same time.
	
	1. Put an order to a shelf.
		# Process
			# Check if the shelf is full. 
				# If it's full
					# Return false
				# If it's not full
					# LOCK the shelf.
					# Double check if the shelf is full, because it's possibly full after the last time checking but before the locking.
						# If it's full
							# Unlock the shelf 
							# Return false
						# If it's not full
							# Put the order to the shelf.
							# Unlock the shelf
							# Return true.
		
	2. If the first time put is failed (got false), then try put it into the overflow shelf, the process is same as <1. Put an order to a shelf.>
	
	3. If put into the overflow shelf is failed (got false), then try to move out an order from the overflow shelf.
		# The moving action should lock 2 shelfs(the overflow shelf and a target shelf), to void the risk that the courier could not find the order in any shelf because the courier also lock the overflow shelf when seeking the order from it, if not found, it will lock the other shelfs one by one while locking the overflow shelf.
		
		# The user choosing process is linear because the second list should not pop up until the user choose an order in the first list or time out. But to void harm the performance, the overflow and other shelfs are not locked all the time in the user choosing process, which means all shelfs are still available to be modified during the user choosing process.
		
		# To avoind confusing, we call the order we try to put in the overflow shelf the Orignal order.
		
		# Process:	
			# Lock the moving action to make it linear.
			# Lock the overflow shelf.
			# Check if the overflow shelf is full, this is necessary because some order might be moved out right after your second time put failure and also no order be put in before you lock it.
				# If it's not full.
					# Put in the Orignal order. 
					# Unlock the overflow shelf. 
					# Unlock the moving action.
					# Return true.
				# If it's full.
					# Print the order list in the console.
					# Unlock the overflow shelf.
					# User have few seconds to choose an order. Note: Because the overflow shelf is not locked, it is still being changed by any kitchen or courier while the user is choosing.
					# The user type in an No# to choose an order or time out.
					# Lock the overflow shelf again.
					# Check if the overflow shelf is full, this is necessary because some order might be move out while the user is choosing.
						# If it's not full.
							# Put in the Orignal order, it's thread safe. 
							# Unlock the overflow shelf, 
							# Unlock the moving action.
							# Return true.
						# If it's full. 3 conditions:
							1: The user choose is invalid.
							2: The user choosed order disappeared in the overflow shelf, it's possible because this order might be pick up by the courier while the user is choosing.
								# For 1 and 2:
									# Randomly take out an order from the overflow shelf and discard it, it's thread safe.
									# Put in the Orignal order. 
									# Unlock the overflow shelf, 
									# Unlock the moving action.
									# Return true.
							3: The user choose order is still in the overflow shelf.
								# Loop the other shelfs one by one to try to put the user choosed order into it. The process is same as the <1. Put an order to a shelf.>
								# Note: The <1. Put an order to a shelf.> is locking a shelf, so there are 2 shelf locked in that instant.
									# Put success
										# Remove the user choose order from the overflow, it's thread.
										# Put in the Orignal order, it's thread safe. 
										# Unlock the overflow shelf, 
										# Unlock the moving action.
										# Return true.
									# Put failed, no shelfs has rooms.
										# Randomly take out an order from the overflow shelf and discard it, it's thread safe because the overflow shelf is still in locked.
										# Put in the Orignal order, it's thread safe. 
										# Unlock the overflow shelf, 
										# Unlock the moving action.
										# Return true.


