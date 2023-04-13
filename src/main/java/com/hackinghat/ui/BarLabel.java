package com.hackinghat.ui;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.css.StyleConverter;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.HorizontalDirection;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextAlignment;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class BarLabel extends AnchorPane implements  Initializable
{
    private final DoubleProperty                          barPosition = new SimpleDoubleProperty();

    @FXML
    private Rectangle                               rectangle;

    @FXML
    private Label                                   label;

    private HorizontalDirection                     horizontalDirection;

    private static class DirectionConverter extends StyleConverter<HorizontalDirection, TextAlignment>
    {
        public DirectionConverter() {}

        HorizontalDirection convert(final TextAlignment alignment) { return alignment == TextAlignment.LEFT ? HorizontalDirection.LEFT : HorizontalDirection.RIGHT; }
        TextAlignment convert(final HorizontalDirection direction) { return direction == HorizontalDirection.LEFT ? TextAlignment.LEFT : TextAlignment.RIGHT; }
    }

    public String getText()
    {
        return label.getText();
    }

    public void setText(final String text)
    {
        label.setText(text);
    }

    public Double getBarPosition()
    {
        return barPosition.getValue();
    }

    public void setBarPosition(final Double barPosition)
    {
        this.barPosition.setValue(barPosition);
    }

    public void setHorizontalDirection(final HorizontalDirection horizontalDirection)
    {
        this.horizontalDirection = horizontalDirection;
    }

    public HorizontalDirection getHorizontalDirection()
    {
        return horizontalDirection;
    }

    @Override
    public void initialize(final URL location, ResourceBundle resources)
    {

        widthProperty().addListener((observableValue, number, t1) -> {
            if (barPosition.getValue() != null)
                rectangle.setWidth(t1.doubleValue()/100.0*barPosition.getValue());
        });
        rectangle.heightProperty().bind(heightProperty());
        label.prefHeightProperty().bind(heightProperty());

//        label.textAlignmentProperty().bind(horizontalDirection, new DirectionConverter());
    }



    public BarLabel()
    {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("BarLabel.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try
        {
            fxmlLoader.load();
        }
        catch (final IOException ioex)
        {
            throw new RuntimeException(ioex);
        }
    }
}
