void initDropdownlist() {
  COMports = controlP5.addDropdownList("COMPort") // Make a dropdown list with all serial ports                      
                      .setPosition(10, 20)
                      .setSize(210, 200)
                      .setCaptionLabel("Select COM port"); // Set the lable of the bar when nothing is selected
  
  customize(COMports); // Setup the dropdownlist

  controlP5.addButton("Connect")
           .setPosition(225, 3)
           .setSize(45, 15);
  
  controlP5.addButton("Disconnect")
           .setPosition(275, 3)
           .setSize(52, 15);
}

void customize(DropdownList ddl) {
  ddl.setBackgroundColor(color(200)); // Set the background color of the line between values
  ddl.setItemHeight(20); // Set the height of each item when the list is opened
  ddl.setBarHeight(15); // Set the height of the bar itself

  ddl.getCaptionLabel().getStyle().marginTop = 3; // Set the top margin of the lable
  ddl.getCaptionLabel().getStyle().marginLeft = 3; // Set the left margin of the lable
  
  ddl.setColorBackground(color(60));
  ddl.setColorActive(color(255, 128));
   
  // Now well add the ports to the list, we use a for loop for that
  for (int i=0; i<Serial.list().length; i++) {
    ddl.addItem(Serial.list()[i], i); // This is the line doing the actual adding of items, we use the current loop we are in to determine what place in the char array to access and what item number to add it as
    if (Serial.list()[i].indexOf("Balanduino") != -1)
      ddl.setValue(i); // Automaticly select the Balanduino balancing robot on Mac OS X and Linux
  }
}

