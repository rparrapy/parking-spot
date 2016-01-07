package nl.tue.iot;

/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
 * <p/>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * <p/>
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.html.
 * <p/>
 * Contributors:
 * Zebra Technologies - initial API and implementation
 * Sierra Wireless, - initial API and implementation
 * Bosch Software Innovations GmbH, - initial API and implementation
 *******************************************************************************/


import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.client.californium.LeshanClient;
import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.request.DeregisterRequest;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.RegisterResponse;
import org.eclipse.leshan.core.response.WriteResponse;

/*
 * To build:
 * mvn assembly:assembly -DdescriptorId=jar-with-dependencies
 * To use:
 * java -jar target/leshan-client-*-SNAPSHOT-jar-with-dependencies.jar 127.0.0.1 5683
 */
public class ParkingSpot {
    private String registrationID;
    private final Location locationInstance = new Location();
    private final FirmwareUpdate firmwareUpdateInstance = new FirmwareUpdate();
    private final MultipleAxisJoystick multipleAxisJoystickInstance = new MultipleAxisJoystick();

    public static void main(final String[] args) {
        if (args.length != 4 && args.length != 2) {
            System.out.println(
                    "Usage:\njava -jar target/parking-spot-*-SNAPSHOT-jar-with-dependencies.jar [ClientIP] [ClientPort] ServerIP ServerPort");
        } else {
            if (args.length == 4)
                new ParkingSpot(args[0], Integer.parseInt(args[1]), args[2], Integer.parseInt(args[3]));
            else
                new ParkingSpot("0", 0, args[0], Integer.parseInt(args[1]));
        }
    }

    public ParkingSpot(final String localHostName, final int localPort, final String serverHostName,
                       final int serverPort) {

        // Initialize object list
        ObjectsInitializer initializer = new ObjectsInitializer();

        initializer.setClassForObject(3, Device.class);
        initializer.setClassForObject(3341, AddressableTextDisplay.class);
        //initializer.setClassForObject(3345, MultipleAxisJoystick.class);
        initializer.setClassForObject(32700, ParkingSpotObject.class);
        initializer.setInstancesForObject(3345, multipleAxisJoystickInstance);
        initializer.setInstancesForObject(6, locationInstance);
        initializer.setInstancesForObject(5, firmwareUpdateInstance);

        //This is VERY hacky, but I don't want to modify Leshan code at the moment
        List<LwM2mObjectEnabler> enablers = (List<LwM2mObjectEnabler>) (List<?>) initializer.createMandatory();
        enablers.add(initializer.create(6));
        enablers.add(initializer.create(5));
        //enablers.add(initializer.create(3341));


        // Create client
        final InetSocketAddress clientAddress = new InetSocketAddress(localHostName, localPort);
        final InetSocketAddress serverAddress = new InetSocketAddress(serverHostName, serverPort);

        final LeshanClient client = new LeshanClient(clientAddress, serverAddress, enablers);

        // Start the client
        client.start();

        // Register to the server
        final String endpointIdentifier = UUID.randomUUID().toString();
        RegisterResponse response = client.send(new RegisterRequest(endpointIdentifier));
        if (response == null) {
            System.out.println("Registration request timeout");
            return;
        }

        System.out.println("Device Registration (Success? " + response.getCode() + ")");
        if (response.getCode() != ResponseCode.CREATED) {
            // TODO Should we have a error message on response ?
            // System.err.println("\tDevice Registration Error: " + response.getErrorMessage());
            System.err.println(
                    "If you're having issues connecting to the LWM2M endpoint, try using the DTLS port instead");
            return;
        }

        registrationID = response.getRegistrationID();
        System.out.println("\tDevice: Registered Client Location '" + registrationID + "'");

        // Deregister on shutdown and stop client.
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                if (registrationID != null) {
                    System.out.println("\tDevice: Deregistering Client '" + registrationID + "'");
                    client.send(new DeregisterRequest(registrationID), 1000);
                    client.stop();
                }
            }
        });

        // Change the location through the Console
        Scanner scanner = new Scanner(System.in);
        System.out.println("Press 'w','a','s','d' to change reported Location.");
        while (scanner.hasNext()) {
            String nextMove = scanner.next();
            locationInstance.moveLocation(nextMove);
        }
        scanner.close();
    }

    public static class Device extends BaseInstanceEnabler {

        public Device() {
            // notify new date each 5 second
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    fireResourcesChange(13);
                }
            }, 5000, 5000);
        }

        @Override
        public ReadResponse read(int resourceid) {
            System.out.println("Read on Device Resource " + resourceid);
            switch (resourceid) {
                case 0:
                    return ReadResponse.success(resourceid, getManufacturer());
                case 1:
                    return ReadResponse.success(resourceid, getModelNumber());
                case 2:
                    return ReadResponse.success(resourceid, getSerialNumber());
                case 3:
                    return ReadResponse.success(resourceid, getFirmwareVersion());
                case 9:
                    return ReadResponse.success(resourceid, getBatteryLevel());
                case 10:
                    return ReadResponse.success(resourceid, getMemoryFree());
                case 11:
                    Map<Integer, Long> errorCodes = new HashMap<>();
                    errorCodes.put(0, getErrorCode());
                    return ReadResponse.success(resourceid, errorCodes, Type.INTEGER);
                case 13:
                    return ReadResponse.success(resourceid, getCurrentTime());
                case 14:
                    return ReadResponse.success(resourceid, getUtcOffset());
                case 15:
                    return ReadResponse.success(resourceid, getTimezone());
                case 16:
                    return ReadResponse.success(resourceid, getSupportedBinding());
                default:
                    return super.read(resourceid);
            }
        }

        @Override
        public ExecuteResponse execute(int resourceid, String params) {
            System.out.println("Execute on Device resource " + resourceid);
            if (params != null && params.length() != 0)
                System.out.println("\t params " + params);
            return ExecuteResponse.success();
        }

        @Override
        public WriteResponse write(int resourceid, LwM2mResource value) {
            System.out.println("Write on Device Resource " + resourceid + " value " + value);
            switch (resourceid) {
                case 13:
                    return WriteResponse.notFound();
                case 14:
                    setUtcOffset((String) value.getValue());
                    fireResourcesChange(resourceid);
                    return WriteResponse.success();
                case 15:
                    setTimezone((String) value.getValue());
                    fireResourcesChange(resourceid);
                    return WriteResponse.success();
                default:
                    return super.write(resourceid, value);
            }
        }

        private String getManufacturer() {
            return "Parking Spot Device";
        }

        private String getModelNumber() {
            return "Model 500";
        }

        private String getSerialNumber() {
            return "LT-500-000-0001";
        }

        private String getFirmwareVersion() {
            return "1.0.0";
        }

        private long getErrorCode() {
            return 0;
        }

        private int getBatteryLevel() {
            final Random rand = new Random();
            return rand.nextInt(100);
        }

        private int getMemoryFree() {
            final Random rand = new Random();
            return rand.nextInt(50) + 114;
        }

        private Date getCurrentTime() {
            return new Date();
        }

        private String utcOffset = new SimpleDateFormat("X").format(Calendar.getInstance().getTime());
        ;

        private String getUtcOffset() {
            return utcOffset;
        }

        private void setUtcOffset(String t) {
            utcOffset = t;
        }

        private String timeZone = TimeZone.getDefault().getID();

        private String getTimezone() {
            return timeZone;
        }

        private void setTimezone(String t) {
            timeZone = t;
        }

        private String getSupportedBinding() {
            return "U";
        }
    }

    public static class Location extends BaseInstanceEnabler {
        private Random random;
        private float latitude;
        private float longitude;
        private Date timestamp;

        public Location() {
            random = new Random();
            latitude = Float.valueOf(random.nextInt(180));
            longitude = Float.valueOf(random.nextInt(360));
            timestamp = new Date();
        }

        @Override
        public ReadResponse read(int resourceid) {
            System.out.println("Read on Location Resource " + resourceid);
            switch (resourceid) {
                case 0:
                    return ReadResponse.success(resourceid, getLatitude());
                case 1:
                    return ReadResponse.success(resourceid, getLongitude());
                case 5:
                    return ReadResponse.success(resourceid, getTimestamp());
                default:
                    return super.read(resourceid);
            }
        }

        public void moveLocation(String nextMove) {
            switch (nextMove.charAt(0)) {
                case 'w':
                    moveLatitude(1.0f);
                    break;
                case 'a':
                    moveLongitude(-1.0f);
                    break;
                case 's':
                    moveLatitude(-1.0f);
                    break;
                case 'd':
                    moveLongitude(1.0f);
                    break;
            }
        }

        private void moveLatitude(float delta) {
            latitude = latitude + delta;
            timestamp = new Date();
            fireResourcesChange(0, 5);
        }

        private void moveLongitude(float delta) {
            longitude = longitude + delta;
            timestamp = new Date();
            fireResourcesChange(1, 5);
        }

        public String getLatitude() {
            return Float.toString(latitude - 90.0f);
        }

        public String getLongitude() {
            return Float.toString(longitude - 180.f);
        }

        public Date getTimestamp() {
            return timestamp;
        }
    }

    public static class FirmwareUpdate extends BaseInstanceEnabler {
        private String packageURI;
        private String update;

        public FirmwareUpdate() {
            packageURI = "";
            update = "";
        }

        @Override
        public ReadResponse read(int resourceid) {
            System.out.println("Read on Firmware Update Resource " + resourceid);
            switch (resourceid) {
                case 1:
                    return ReadResponse.success(resourceid, getPackageURI());
                default:
                    return super.read(resourceid);
            }
        }

        @Override
        public ExecuteResponse execute(int resourceid, String params) {
            System.out.println("Execute on Firmware Update resource " + resourceid);
            if (resourceid == 2)
                //TODO implement firmware update here
                System.out.println("\t params " + params);
            return ExecuteResponse.success();
        }

        @Override
        public WriteResponse write(int resourceid, LwM2mResource value) {
            System.out.println("Write on Firmware Update Resource " + resourceid + " value " + value);
            switch (resourceid) {
                case 13:
                    return WriteResponse.notFound();
                case 1:
                    setPackageURI((String) value.getValue());
                    fireResourcesChange(resourceid);
                    return WriteResponse.success();
                default:
                    return super.write(resourceid, value);
            }
        }

        public String getPackageURI() {
            return packageURI;
        }

        public void setPackageURI(String packageURI) {
            this.packageURI = packageURI;
        }

        public String getUpdate() {
            return update;
        }

        public void setUpdate(String update) {
            this.update = update;
        }



    }

    public static class AddressableTextDisplay extends BaseInstanceEnabler {
        private String text;

        public AddressableTextDisplay() {
            text = "green";
        }

        @Override
        public ReadResponse read(int resourceid) {
            System.out.println("Read on Firmware Update Resource " + resourceid);
            switch (resourceid) {
                case 5527:
                    return ReadResponse.success(resourceid, getText());
                default:
                    return super.read(resourceid);
            }
        }

        @Override
        public WriteResponse write(int resourceid, LwM2mResource value) {
            System.out.println("Write on Firmware Update Resource " + resourceid + " value " + value);
            switch (resourceid) {
                case 5527:
                    setText((String) value.getValue());
                    fireResourcesChange(resourceid);
                    return WriteResponse.success();
                default:
                    return super.write(resourceid, value);
            }
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    public static class  MultipleAxisJoystick  extends BaseInstanceEnabler {
        private int digitalInputCounter;
        private int yValue;

        public MultipleAxisJoystick() {
            digitalInputCounter = 0;
            yValue = -100;
        }

        @Override
        public ReadResponse read(int resourceid) {
            System.out.println("Read on Firmware Update Resource " + resourceid);
            switch (resourceid) {
                case 5501:
                    return ReadResponse.success(resourceid, getDigitalInputCounter());
                case 5703:
                    return ReadResponse.success(resourceid, getyValue());
                default:
                    return super.read(resourceid);
            }
        }

        public int getyValue() {
            return yValue;
        }

        public void setyValue(int yValue) {
            this.yValue = yValue;
        }

        public int getDigitalInputCounter() {
            return digitalInputCounter;
        }

        public void setDigitalInputCounter(int digitalInputCounter) {
            this.digitalInputCounter = digitalInputCounter;
        }
    }

    public static class  ParkingSpotObject  extends BaseInstanceEnabler {
        private String id;
        private String state;
        private String vehicleId;
        private float billingRate;

        public ParkingSpotObject() {
            this.id = "Parking-Spot-22";
            this.state = "free";
            this.vehicleId = "";
            this.billingRate = 0.01f;
        }

        @Override
        public ReadResponse read(int resourceid) {
            System.out.println("Read on ParkingSpotObject Resource " + resourceid);
            switch (resourceid) {
                case 32800:
                    return ReadResponse.success(resourceid, getId());
                case 32801:
                    return ReadResponse.success(resourceid, getState());
                case 32802:
                    return ReadResponse.success(resourceid, getVehicleId());
                case 32803:
                    return ReadResponse.success(resourceid, getBillingRate());
                default:
                    return super.read(resourceid);
            }
        }

        @Override
        public WriteResponse write(int resourceid, LwM2mResource value) {
            System.out.println("Write on ParkingSpotObject Resource " + resourceid + " value " + value);
            switch (resourceid) {
                case 32801:
                    setState((String) value.getValue());
                    fireResourcesChange(resourceid);
                    return WriteResponse.success();
                case 32802:
                    setVehicleId((String) value.getValue());
                    fireResourcesChange(resourceid);
                    return WriteResponse.success();
                case 32803:
                    setBillingRate((Float) value.getValue());
                    fireResourcesChange(resourceid);
                    return WriteResponse.success();
                default:
                    return super.write(resourceid, value);
            }
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public String getVehicleId() {
            return vehicleId;
        }

        public void setVehicleId(String vehicleId) {
            this.vehicleId = vehicleId;
        }

        public float getBillingRate() {
            return billingRate;
        }

        public void setBillingRate(float billingRate) {
            this.billingRate = billingRate;
        }
    }

}