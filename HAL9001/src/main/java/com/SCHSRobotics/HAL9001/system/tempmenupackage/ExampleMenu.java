package com.SCHSRobotics.HAL9001.system.tempmenupackage;

import android.util.Log;

import com.SCHSRobotics.HAL9001.util.misc.Button;

public class ExampleMenu extends ListViewMenu {
    @Override
    protected void init(Payload payload) {
        if(payload.idPresent(TextSelectionMenu.ENTERED_TEXT_ID)) {
            Log.wtf("Entered text", (String) payload.get(TextSelectionMenu.ENTERED_TEXT_ID));
        }

        //selectionZone = new SelectionZone(1,2);
        addItem(new ViewButton("# | Fun Times")
                .onClick(new Button<>(1, Button.BooleanInputs.x), (DataPacket packet) -> {
                    gui.inflate(new ExampleMenu2());
                })
        );
        addItem(new TextElement("# | LOOK! its new text!"));
        addItem(new ViewButton("# | More fun stuff")
                .onClick(new Button<>(1, Button.BooleanInputs.a), (DataPacket packet) -> {
                    gui.inflate(new ExampleMenu2());
                }));
        addItem(new EntireViewButton()
                .onClick(new Button<>(1, Button.BooleanInputs.b), (DataPacket packet) -> {
                    gui.inflate(new ExampleMenu3());
                }));
        addItem(new TextElement("# | LOOK! its new text!"));
        addItem(new TextElement("# | LOOK! its new text!"));
        addItem(new TextElement("# | LOOK! its new text!"));
        addItem(new EntireViewButton()
                 .onClick(new Button<>(1, Button.BooleanInputs.y), (DataPacket packet) -> {
                    gui.forward();
                 }));
        addItem(new TextElement("# | LOOK! its new text!"));
        addItem(new TextElement("# | LOOK! its new text!"));
        addItem(new TextElement("# | LOOK! its new text!"));
        addItem(new TextElement("# | LOOK! its new text!"));
        addItem(new TextElement("# | You found the end of the page!"));
    }
}
