package com.hackinghat.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class Controller {

    @FXML
    TextField inputField;

    @FXML
    Label label;

    @FXML
    Button applyButton;

    public void applyButtonClicked ()
    {
        label.setText(inputField.getText());
    }
}