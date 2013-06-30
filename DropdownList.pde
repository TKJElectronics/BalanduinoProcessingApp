void initDropdownlist() {  
  dropdownList = controlP5.addDropdownList("SerialPort"); // Make a dropdown list with all serial ports
  
  dropdownList.setPosition(10, 20);
  dropdownList.setSize(210, 200);
  dropdownList.setCaptionLabel("Select serial port"); // Set the lable of the bar when nothing is selected
  
  dropdownList.setBackgroundColor(color(200)); // Set the background color of the line between values
  dropdownList.setItemHeight(20); // Set the height of each item when the list is opened
  dropdownList.setBarHeight(15); // Set the height of the bar itself
  
  dropdownList.getCaptionLabel().getStyle().marginTop = 3; // Set the top margin of the lable
  dropdownList.getCaptionLabel().getStyle().marginLeft = 3; // Set the left margin of the lable
  
  dropdownList.setColorBackground(color(60));
  dropdownList.setColorActive(color(255, 128));
  
  // Now well add the ports to the list, we use a for loop for that
  for (int i=0; i<Serial.list().length; i++) {
    dropdownList.addItem(Serial.list()[i], i); // This is the line doing the actual adding of items, we use the current loop we are in to determine what place in the char array to access and what item number to add it as
    if (Serial.list()[i].indexOf("Balanduino") != -1 && dropdownList.getValue() == 0) // Check for the "Balanduino" substring and make sure it is not already set
      dropdownList.setValue(i); // Automaticly select the Balanduino balancing robot on Mac OS X and Linux
  }
  
  addMouseWheelListener(new MouseWheelListener() { // Add a mousewheel listener to scroll the dropdown list
    public void mouseWheelMoved(MouseWheelEvent mwe) {
      dropdownList.scroll(mwe.getWheelRotation() > 0 ? 1 : 0); // Scroll the dropdownlist using the mousewheel
  }});

  controlP5.addButton("connect")
           .setPosition(225, 3)
           .setSize(45, 15);
           
  controlP5.addButton("disconnect")
           .setPosition(275, 3)
           .setSize(52, 15);
}
