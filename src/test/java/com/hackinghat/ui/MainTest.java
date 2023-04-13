package com.hackinghat.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.loadui.testfx.GuiTest;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit.ApplicationTest;

import static junit.framework.TestCase.assertEquals;

public class MainTest extends ApplicationTest
{
    @Override
    public void start (Stage stage) throws Exception {
        Parent mainNode = FXMLLoader.load(Main.class.getResource("application.fxml"));
        stage.setScene(new Scene(mainNode));
        stage.show();
        stage.toFront();
    }
    @Before
    public void setUp () {
    }

    @Test
    @Ignore
    public void testEnglishInput () {
        Label label = GuiTest.find("#label");
        clickOn("#inputField");
        write("This is a test!");
        clickOn("#applyButton");
        assertEquals("This is a test!", label.getText());
    }

    @After
    public void tearDown () throws Exception {
        FxToolkit.hideStage();
        release(new KeyCode[]{});
        release(new MouseButton[]{});
    }
}
