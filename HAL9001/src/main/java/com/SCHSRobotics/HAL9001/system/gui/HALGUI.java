package com.SCHSRobotics.HAL9001.system.gui;

import com.SCHSRobotics.HAL9001.system.gui.event.DataPacket;
import com.SCHSRobotics.HAL9001.system.gui.event.GamepadEventGenerator;
import com.SCHSRobotics.HAL9001.system.gui.viewelement.eventlistener.EntireViewButton;
import com.SCHSRobotics.HAL9001.system.robot.Robot;
import com.SCHSRobotics.HAL9001.util.control.Button;
import com.SCHSRobotics.HAL9001.util.control.CustomizableGamepad;
import com.SCHSRobotics.HAL9001.util.control.Toggle;
import com.SCHSRobotics.HAL9001.util.exceptions.DumpsterFireException;
import com.SCHSRobotics.HAL9001.util.exceptions.ExceptionChecker;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A GUI class to control the menu system.
 * This class follows a singleton pattern.
 *
 * @author Cole Savage, Level Up
 * @since 1.1.0
 * @version 1.1.0
 *
 * Creation Date: 4/29/20
 *
 * @see HALMenu
 */
public class HALGUI {
    //A queue to store all currently active trees of menus.
    private Queue<Stack<HALMenu>> menuStacks;
    //A queue to store all controls for the cursor. Controls apply to each menu tree. Controls default to the dpad.
    private Queue<EntireViewButton> cursorControlQueue;
    //The current menu stack. Shows history of menus visited within a tree. Does not contain currently active menu.
    private Stack<HALMenu> currentStack;
    //Stores backups of "undo-ed" menus in a tree so that they can be "redo-ed".
    private Stack<HALMenu> forwardStack;
    //The currently selected menu
    private HALMenu currentMenu;
    //The robot the gui uses to send messages to telemetry.
    private Robot robot;
    //The last millisecond timestamp of when the render function was called.
    private long lastRenderTime;
    //The customizable gamepad used to cycle between menu keys.
    private CustomizableGamepad cycleControls;
    //The button used to cycle between menu keys.
    private Button<Boolean> cycleButton;
    //The toggle object used to correctly track button presses.
    private Toggle cycleToggle, clearToggle;
    //Singleton instance of the gui.
    private static HALGUI INSTANCE = new HALGUI();

    private static final int DEFAULT_TRANSMISSION_INTERVAL_MS = 250;
    private static final int DEFAULT_HAL_TRANSMISSION_INTERVAL_MS = 50;

    /**
     * The private GUI constructor. Initializes the queues, the render timestamp, and the cycle toggle.
     */
    private HALGUI() {
    }

    /**
     * Gets the static instance of the gui.
     *
     * @return The static instance of the gui.
     */
    @Contract(pure = true)
    public static HALGUI getInstance() {
        return INSTANCE;
    }

    /**
     * Initializes the GUI with a robot and menu-cycling button.
     *
     * @param robot The robot that the gui will use to control the telemetry.
     * @param cycleButton The button used to cycle between menu trees.
     */
    public void setup(Robot robot, Button<Boolean> cycleButton) {
        this.robot = robot;
        setCycleButton(cycleButton);

        cycleControls = new CustomizableGamepad(robot);
        menuStacks = new LinkedBlockingQueue<>();
        cursorControlQueue = new LinkedBlockingQueue<>();
        forwardStack = new Stack<>();
        lastRenderTime = 0;
        cycleToggle = new Toggle(Toggle.ToggleTypes.trueOnceToggle, false);
        clearToggle = new Toggle(Toggle.ToggleTypes.flipToggle, false);
        robot.telemetry.setMsTransmissionInterval(DEFAULT_HAL_TRANSMISSION_INTERVAL_MS);
        robot.telemetry.setDisplayFormat(Telemetry.DisplayFormat.MONOSPACE);
        GamepadEventGenerator.getInstance().reset();
    }

    /**
     * Adds a menu at the root of a new menu tree. This creates a new menu tree.
     *
     * @param menu The root menu of the new tree.
     */
    public void addRootMenu(@NotNull HALMenu menu) {
        currentMenu = menu;
        currentStack = new Stack<>();
        currentStack.push(currentMenu);
        forwardStack.clear();
        menuStacks.add(currentStack);
        cursorControlQueue.add(new EntireViewButton()
                .onClick(new Button<>(1, Button.BooleanInputs.dpad_up), (DataPacket packet) -> currentMenu.cursorUp())
                .onClick(new Button<>(1, Button.BooleanInputs.dpad_down), (DataPacket packet) -> currentMenu.cursorDown())
                .onClick(new Button<>(1, Button.BooleanInputs.dpad_left), (DataPacket packet) -> currentMenu.cursorLeft())
                .onClick(new Button<>(1, Button.BooleanInputs.dpad_right), (DataPacket packet) -> currentMenu.cursorRight()));
        currentMenu.addItem(cursorControlQueue.peek());
        currentMenu.init(currentMenu.payload);
    }

    /**
     * Adds a new menu to the current tree and displays it. This does not create a new tree, and instead adds on to the existing tree.
     *
     * @param menu The menu to add to the new tree and display.
     */
    public void inflate(@NotNull HALMenu menu) {
        inflate(menu, menu.payload);
    }

    public void inflate(HALMenu menu, Payload payload) {
        forwardStack.clear();
        currentMenu = menu;
        currentStack.push(currentMenu);
        currentMenu.clearElements();
        currentMenu.addItem(cursorControlQueue.peek());
        currentMenu.init(payload);
    }

    public void inflate(Class<HALMenu> menuClass, Payload payload) {
        try {
            inflate(menuClass.getConstructor(Payload.class).newInstance(payload));
        }
        catch (NoSuchMethodException e) {
            throw new DumpsterFireException("No constructor taking a payload as input was found.");
        }
        catch (IllegalAccessException e) {
            throw new DumpsterFireException("Constructor taking payload as input was not public.");
        }
        catch (InstantiationException e) {
            throw new DumpsterFireException("Could not instantiate menu with given payload.");
        }
        catch (InvocationTargetException e) {
            throw new DumpsterFireException(e.getMessage());
        }
    }

    /**
     * Renders the currently active menu.
     */
    public void renderCurrentMenu() {
        if (isInitialized() && !menuStacks.isEmpty()) {
            boolean forceCursorUpdate = currentMenu.updateListeners();

            if (System.currentTimeMillis() - lastRenderTime >= currentMenu.getCursorBlinkSpeedMs() || forceCursorUpdate) {
                currentMenu.notifyForceCursorUpdate(forceCursorUpdate);
                currentMenu.render();
                robot.telemetry.update();
                lastRenderTime = System.currentTimeMillis();
            }

            cycleToggle.updateToggle(cycleControls.getInput(cycleButton));
            if (cycleToggle.getCurrentState()) {
                menuStacks.add(menuStacks.poll());
                currentStack = menuStacks.peek();
                ExceptionChecker.assertNonNull(currentStack, new DumpsterFireException("The stack of menus that you tried to switch to is null. This state should be unreachable, what did you do?!?"));
                currentMenu = currentStack.peek();
                cursorControlQueue.add(cursorControlQueue.poll());
                forwardStack.clear();
            }
        } else if (isInitialized() && clearToggle.getCurrentState()) {

            robot.telemetry.addLine("");
            robot.telemetry.update();

            clearToggle.updateToggle(false);
            clearToggle.updateToggle(true);
        }
    }

    public void setCursorControls(Button<Boolean> upButton, Button<Boolean> downButton, Button<Boolean> leftButton, Button<Boolean> rightButton) {
        ExceptionChecker.assertFalse(upButton.equals(downButton) ||
                upButton.equals(leftButton) ||
                upButton.equals(rightButton) ||
                downButton.equals(leftButton) ||
                downButton.equals(rightButton) ||
                leftButton.equals(rightButton), new DumpsterFireException("All cursor controls must be unique"));

        EntireViewButton newCursor = new EntireViewButton()
                .onClick(upButton, (DataPacket packet) -> currentMenu.cursorUp())
                .onClick(downButton, (DataPacket packet) -> currentMenu.cursorDown())
                .onClick(leftButton, (DataPacket packet) -> currentMenu.cursorLeft())
                .onClick(rightButton, (DataPacket packet) -> currentMenu.cursorRight());

        cursorControlQueue.poll();
        cursorControlQueue.add(newCursor);
        for (int i = 0; i < cursorControlQueue.size() - 1; i++) {
            cursorControlQueue.add(cursorControlQueue.poll());
        }

        currentMenu.setCursor(newCursor);
    }

    public void back(@NotNull Payload payload) {
        if(!currentStack.isEmpty()) {
            forwardStack.push(currentStack.pop());
            currentMenu = currentStack.peek();
            currentMenu.clearElements();
            currentMenu.addItem(cursorControlQueue.peek());
            currentMenu.init(payload);
        }
    }

    public void back() {
        back(new Payload());
    }

    public void forward(@NotNull Payload payload) {
        if(!forwardStack.isEmpty()) {
            currentStack.push(currentMenu);
            currentMenu = forwardStack.pop();
            currentMenu.clearElements();
            currentMenu.addItem(cursorControlQueue.peek());
            currentMenu.init(payload);
        }
    }

    public void forward() {
        forward(new Payload());
    }

    public void removeCurrentStack() {
        menuStacks.poll();
        cursorControlQueue.poll();

        forwardStack.clear();
        currentStack = menuStacks.peek();

        if (currentStack == null) {
            clearToggle.updateToggle(true);
        } else {
            currentMenu = currentStack.peek();
        }
    }

    public void stop() {
        if (robot != null) {
            robot.telemetry.setMsTransmissionInterval(DEFAULT_TRANSMISSION_INTERVAL_MS);
            robot.telemetry.setDisplayFormat(Telemetry.DisplayFormat.CLASSIC);
        }
        currentStack = null;
        currentMenu = null;
        robot = null;
        cycleControls = null;
        cycleButton = null;
    }

    public void setCycleButton(Button<Boolean> cycleButton) {
        this.cycleButton = cycleButton;
    }

    public int getCursorX() {
        if (currentMenu != null) {
            return currentMenu.getCursorX();
        }
        return 0;
    }

    public int getCursorY() {
        if (currentMenu != null) {
            return currentMenu.getCursorY();
        }
        return 0;
    }

    /**
     * Gets the robot the gui is using to control the telemetry.
     *
     * @return The robot that the gui will use to control the telemetry.
     */
    public Robot getRobot() {
        return robot;
    }

    public boolean isInitialized() {
        return robot != null;
    }
}