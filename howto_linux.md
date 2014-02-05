To use the app with Linux you will first need to figure out the Bluetooth address of the Balanduino by running the following command:

```
$ hcitool scan
  Scanning ...
    00:1A:7D:DA:71:0F	Balanduino
```

After that you will need to add a entry in ```/etc/bluetooth/rfcomm.conf``` like so:

```
$ nano /etc/bluetooth/rfcomm.conf
```


```
#
# RFCOMM configuration file.
#

rfcomm0 {
	# Automatically bind the device at startup
	bind yes;

	# Bluetooth address of the device
	device 00:1A:7D:DA:71:0F;

	# RFCOMM channel for the connection
	channel	1;

	# Description of the connection
	comment "Balanduino";
}
```

Remember to modify the Bluetooth address to match the one you found using hcitool.

After that you will need to connect to the robot like so:

```
$ sudo rfcomm connect 0
  Connected /dev/rfcomm0 to 00:1A:7D:DA:71:0F on channel 1
  Press CTRL-C for hangup
```

After that simply run the application like so:

```
$ sudo ./BalanduinoProcessingApp
```


If you get the following error message:

```
Can't create RFCOMM TTY: Address already in use
```

Then try to run the following command:

```
$ sudo rfcomm release 0
```