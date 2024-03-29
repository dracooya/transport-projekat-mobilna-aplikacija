package com.example.uberapp_tim9.passenger.fragments;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.transition.Slide;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uberapp_tim9.R;
import com.example.uberapp_tim9.driver.notificationManager.NotificationService;
import com.example.uberapp_tim9.map.MapInit;
import com.example.uberapp_tim9.model.Message;
import com.example.uberapp_tim9.model.MessageType;
import com.example.uberapp_tim9.model.Ride;
import com.example.uberapp_tim9.model.User;
import com.example.uberapp_tim9.model.dtos.DriverDTO;
import com.example.uberapp_tim9.model.dtos.LocationDTO;
import com.example.uberapp_tim9.model.dtos.MessageSimpleDTO;
import com.example.uberapp_tim9.model.dtos.PassengerIdEmailDTO;
import com.example.uberapp_tim9.model.dtos.RejectionReasonDTO;
import com.example.uberapp_tim9.model.dtos.RideCreatedDTO;
import com.example.uberapp_tim9.model.dtos.RideCreationNowDTO;
import com.example.uberapp_tim9.model.dtos.RouteDTO;
import com.example.uberapp_tim9.passenger.PassengerMainActivity;
import com.example.uberapp_tim9.passenger.PassengerReviewRideActivity;
import com.example.uberapp_tim9.passenger.adapters.MessagesListAdapter;
import com.example.uberapp_tim9.shared.LoggedUserInfo;
import com.example.uberapp_tim9.shared.directions.RouteDrawer;
import com.example.uberapp_tim9.shared.rest.RestApiManager;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.reactivex.disposables.Disposable;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PassengerMainFragment extends Fragment{

    private static Button createButton;
    private static FrameLayout passenger_main_container;
    private static Button l4;
    private static Button l_add;
    private static Button r_add;
    private static Button l2;
    private static Button l3;
    private static Button r1;
    private static Button r2;
    private static Button r3;
    private static RelativeLayout stepp_add;
    private static RelativeLayout stepp1;
    private static RelativeLayout stepp2;
    private static RelativeLayout stepp3;
    private static RelativeLayout stepp4;
    private static Button panic;
    private static Button inconsistency;
    private static TextView rideInfoLabel;
    private static Button callDriver;
    private static Button messageDriver;
    private static TextView timerLabel;
    public static RideCreatedDTO currentRide;
    public static DriverDTO currentRideDriver;
    private static Handler timerHandler = new Handler();
    private static long startTime = 0;
    private static FrameLayout panicOverlay;
    private static Button closeOverlay;
    private static Button panicSend;
    private static Button closeMessageOverlay;
    private static FrameLayout messageOverlay;
    private static Button messageSend;
    private static TextInputEditText msg;

    private static TextInputEditText startLocationInput;
    private static TextInputEditText endLocationInput;
    private static CheckBox babyCheckBox;
    private static CheckBox petCheckBox;
    private static RadioButton standardVehicleRadioButton;
    private static RadioButton luxuryVehicleRadioButton;
    private static RadioButton vanVehicleRadioButton;
    private static CheckBox timeCheckBox;

    private static int msg_id = 4;
    private static Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long millis = System.currentTimeMillis() - startTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;
            timerLabel.setText("Trajanje vožnje: " + String.format("%02d:%02d", minutes, seconds));
            timerHandler.postDelayed(this, 500);
        }
    };

    public PassengerMainFragment() {}

    public static PassengerMainFragment newInstance(String param1, String param2) {
        PassengerMainFragment fragment = new PassengerMainFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MapInit mapUtils = new MapInit();

        //Subscribe to events from websocket (ride has started)
        Disposable subscriptionRideStarted = PassengerMainActivity.socketsConfiguration.stompClient.topic("/ride-started/notification").subscribe(message ->
                {
                    Gson gson = new GsonBuilder().registerTypeAdapter(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
                        @Override
                        public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                                throws JsonParseException {
                            return LocalDateTime.parse(json.getAsString());
                        }
                    }).create();
                    RideCreatedDTO retrieved = gson.fromJson(message.getPayload(), new TypeToken<RideCreatedDTO>(){}.getType());
                    currentRide = retrieved;
                    Call<ResponseBody> getDriverDetails = RestApiManager.restApiInterfacePassenger.getDriverDetails(Integer.toString(retrieved.getDriver().getId()));
                    getDriverDetails.enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            if (response.code() == 200){
                                try {
                                    DriverDTO rideDriver = new Gson().fromJson(response.body().string(), new TypeToken<DriverDTO>(){}.getType());
                                    currentRideDriver = rideDriver;
                                    updateUI(false);
                                    toggleReservationFormVisibility(true);
                                    LatLng departure = null,destination = null;
                                    for(RouteDTO route : currentRide.getLocations()) {
                                        departure = new LatLng(route.getDeparture().getLatitude(),route.getDeparture().getLongitude());
                                        destination = new LatLng(route.getDestination().getLatitude(),route.getDestination().getLongitude());
                                    }
                                    Marker currentRideVehicle = MapFragment.driversMarkers.get(currentRide.getDriver().getId());
                                    currentRideVehicle.setIcon(MapFragment.BitmapFromVector(getActivity(), R.drawable.redcar));
                                    mapUtils.DrawRoute(departure,destination,MapFragment.map);
                                    mapUtils.simulateRoute(departure,
                                                           destination,
                                                           MapFragment.driversMarkers.get(currentRide.getDriver().getId()),
                                                 false,
                                                  false,
                                                  -1,
                                            false,
                                            600,
                                            false,null);
                                }
                                catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            Log.d("REZ", t.getMessage() != null?t.getMessage():"error");
                        }
                    });
                },
                throwable -> Log.e(TAG, throwable.getMessage()));

        Disposable subscriptionRideEnded = PassengerMainActivity.socketsConfiguration.stompClient.topic("/ride-ended/notification").subscribe(message ->
                {
                    List<Integer> passengersId  = new Gson().fromJson(message.getPayload(), new TypeToken<List<Integer>>(){}.getType());
                    if(passengersId.contains(LoggedUserInfo.id)) {
                        NotificationService.createRideEndedNotification(PassengerMainActivity.CHANNEL_ID,getActivity());
                        updateUI(true);
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                toggleReservationFormVisibility(false);
                                MapFragment.clearCurrentRoute();
                                Marker currentRideVehicle = MapFragment.driversMarkers.get(currentRide.getDriver().getId());
                                currentRideVehicle.setIcon(MapFragment.BitmapFromVector(getActivity(), R.drawable.greencar));
                                RouteDrawer.clearPolyLine();
                                for (Polyline polyline : MapFragment.polylines) {
                                    polyline.remove();
                                }
                            }
                        });
                        startActivity(new Intent(getActivity(), PassengerReviewRideActivity.class).putExtra("rideId",currentRide.getId()));
                        RouteDrawer.clearPolyLine();
                        for (Polyline polyline : MapFragment.polylines) {
                            polyline.remove();
                        }
                    }
                },
                throwable -> Log.e(TAG, throwable.getMessage()));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_passenger_main, container, false);
        FragmentManager fm = getChildFragmentManager();
        MapFragment mapFragment = new MapFragment(false);
        FragmentTransaction fragmentTransaction = fm.beginTransaction();
        fragmentTransaction.add(R.id.map_container, mapFragment);
        fragmentTransaction.commit();
        fm.executePendingTransactions();

        UpdateCreate(v);

        panic = v.findViewById(R.id.panic_button);
        panic.setOnClickListener(view -> {updatePanicOverlay(false);});
        panicSend = v.findViewById(R.id.panic_message_send_button);
        closeOverlay = v.findViewById(R.id.login_button);
        closeMessageOverlay = v.findViewById(R.id.close_message_overlay_button);
        messageOverlay = v.findViewById(R.id.message_overlay);
        msg = v.findViewById(R.id.email_input);
        closeMessageOverlay.setOnClickListener(view -> {
            updateMessagesOverlay(true);
        });

        closeOverlay.setOnClickListener(view -> {updatePanicOverlay(true);});
        panicSend.setOnClickListener(view ->{
            TextInputEditText reason = v.findViewById(R.id.panic_message);
            String reasonText = reason.getText().toString().trim();
            if(reasonText.length() == 0) {
                reason.setError(getString(R.string.zeroLengthError));
            } else {
                String rideId = Integer.toString(currentRide.getId());
                RejectionReasonDTO reasonDTO = new RejectionReasonDTO(reasonText);
                Call<ResponseBody> call = RestApiManager.restApiInterfacePassenger.panic(rideId,reasonDTO);
                call.enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (response.code() == 200){
                            Toast.makeText(getActivity(), "Panic uspešno poslat!", Toast.LENGTH_SHORT).show();
                        }
                        else {
                            Toast.makeText(getActivity(), "Vožnja ne postoji!", Toast.LENGTH_SHORT).show();
                        }
                        updatePanicOverlay(true);
                        panic.setVisibility(View.INVISIBLE);
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        Log.d("REZ", t.getMessage() != null?t.getMessage():"error");
                    }
                });
            }
        });

        inconsistency = v.findViewById(R.id.inconsistency_button);
        inconsistency.setOnClickListener(view -> {
            PassengerMainActivity.socketsConfiguration.stompClient.send("/topic/inconsistency",Integer.toString(currentRide.getId())).subscribe();
            inconsistency.setVisibility(View.INVISIBLE);
            Toast.makeText(getActivity(), "Greška uspešno prijavljena!", Toast.LENGTH_SHORT).show();
        });
        rideInfoLabel = v.findViewById(R.id.ride_info_label);
        callDriver = v.findViewById(R.id.call_button);
        messageDriver = v.findViewById(R.id.message_button);
        timerLabel = v.findViewById(R.id.timer_label);
        panicOverlay = v.findViewById(R.id.panic_overlay);

        startLocationInput = v.findViewById(R.id.start_location_input);
        endLocationInput = v.findViewById(R.id.end_location_input);
        babyCheckBox = v.findViewById(R.id.baby_checkbox);
        petCheckBox = v.findViewById(R.id.pet_checkbox);
        standardVehicleRadioButton = v.findViewById(R.id.standard_vehicle_radio_button);
        luxuryVehicleRadioButton = v.findViewById(R.id.luxury_vehicle_radio_button);
        vanVehicleRadioButton = v.findViewById(R.id.van_vehicle_radio_button);
        timeCheckBox = v.findViewById(R.id.time_checkbox);

        MessagesListAdapter messagesListAdapter = new MessagesListAdapter(LoggedUserInfo.id);
        RecyclerView messagesList = v.findViewById(R.id.messages_list);
        LinearLayoutManager messagesLlm = new LinearLayoutManager(getActivity());
        messagesList.setLayoutManager(messagesLlm);
        messagesList.setAdapter(messagesListAdapter);
        messageSend = v.findViewById(R.id.message_send_button);
        Disposable message = PassengerMainActivity.socketsConfiguration.stompClient.topic("/message/notification").subscribe(payload ->
                {
                    MessageSimpleDTO message_received = PassengerMainActivity.gson.fromJson(payload.getPayload(), new TypeToken<MessageSimpleDTO>(){}.getType());
                    if(message_received.getSender() == LoggedUserInfo.id || message_received.getReceiver() == LoggedUserInfo.id) {
                        messagesListAdapter.getmMessageList().add(message_received);
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                if(message_received.getSender() == LoggedUserInfo.id) {
                                    msg.setText("");
                                }
                                messagesListAdapter.notifyItemInserted(messagesListAdapter.getmMessageList().size() - 1);
                            }
                        });
                    }
                },
                throwable -> Log.e(TAG, throwable.getMessage()));

        messageSend.setOnClickListener(view -> {
            MessageSimpleDTO message_new = new MessageSimpleDTO(msg_id,
                    LoggedUserInfo.id,
                    4,
                    msg.getText().toString(),
                    LocalDateTime.now(),
                    MessageType.VOZNJA,
                    currentRide.getId());
            msg_id++;
            PassengerMainActivity.socketsConfiguration.stompClient.send("/topic/message",PassengerMainActivity.gson.toJson(message_new)).subscribe();
        });

        if (getActivity() != null && getActivity().getIntent() != null) {
            Bundle extras = getActivity().getIntent().getExtras();
            if (extras != null) {
                Boolean orderNow = (Boolean) extras.get("orderNow");
                timeCheckBox.setChecked(orderNow);

                String departure = (String) extras.get("departure");
                startLocationInput.setText(departure);
                String destination = (String) extras.get("destination");
                endLocationInput.setText(destination);

                String vehicleType = (String) extras.get("vehicleType");
                if (vehicleType != null) {
                    switch (vehicleType) {
                        case "STANDARD":
                            standardVehicleRadioButton.setChecked(true);
                            luxuryVehicleRadioButton.setChecked(false);
                            vanVehicleRadioButton.setChecked(false);
                            break;
                        case "LUXURY":
                            luxuryVehicleRadioButton.setChecked(true);
                            standardVehicleRadioButton.setChecked(false);
                            vanVehicleRadioButton.setChecked(false);
                            break;
                        case "VAN":
                            vanVehicleRadioButton.setChecked(true);
                            standardVehicleRadioButton.setChecked(false);
                            luxuryVehicleRadioButton.setChecked(false);
                            break;
                    }
                }

                Boolean babyTransport = (Boolean) extras.get("babyTransport");
                babyCheckBox.setChecked(babyTransport);
                Boolean petTransport = (Boolean) extras.get("petTransport");
                petCheckBox.setChecked(petTransport);
            }
        }
        return v;
    }

    public void UpdateCreate(View v){
        createButton = v.findViewById(R.id.create_ride_button);
        r1 = v.findViewById(R.id.right_button_step1);
        r2 = v.findViewById(R.id.right_button_step2);
        r3 = v.findViewById(R.id.right_button_step3);
        r_add = v.findViewById(R.id.right_button_step_add);
        l2 = v.findViewById(R.id.left_button_step2);
        l3 = v.findViewById(R.id.left_button_step3);
        l4 = v.findViewById(R.id.left_button_step4);
        l_add = v.findViewById(R.id.left_button_step_add);
        stepp1 = v.findViewById(R.id.step1_container);
        stepp2 = v.findViewById(R.id.step2_container);
        stepp3 = v.findViewById(R.id.step3_container);
        stepp4 = v.findViewById(R.id.step4_container);
        stepp_add = v.findViewById(R.id.step_add_container);
        passenger_main_container = v.findViewById(R.id.passenger_main_container);
        createButton.setOnClickListener(view -> {
            createRide(v);
        });
        /*((TimePicker)v.findViewById(R.id.timePicker_passenger_stepper)).setOnTimeChangedListener((view, hourOfDay, minute) -> {
            Calendar datetime = Calendar.getInstance();
            Calendar c = Calendar.getInstance();
            datetime.set(Calendar.HOUR_OF_DAY, hourOfDay);
            datetime.set(Calendar.MINUTE, minute);

            if (datetime.getTimeInMillis() <= c.getTimeInMillis() || (datetime.getTimeInMillis() + 18000000) >= c.getTimeInMillis()) {

                Toast.makeText(getActivity().getApplicationContext(), "Pogrešno vreme", Toast.LENGTH_LONG).show();
            }
        });*/

        r1.setOnClickListener(view -> {stepShow(stepp1, stepp2, false);});
        r2.setOnClickListener(view -> {stepShow(stepp2, stepp_add, false);});
        r_add.setOnClickListener(view -> {stepShow(stepp_add, stepp3, false);});
        r3.setOnClickListener(view -> {stepShow(stepp3, stepp4, false);});


        l2.setOnClickListener(view -> {stepShow(stepp2, stepp1, true);});
        l_add.setOnClickListener(view -> {stepShow(stepp_add, stepp2, true);});
        l3.setOnClickListener(view -> {stepShow(stepp3, stepp_add, true);});
        l4.setOnClickListener(view -> {stepShow(stepp4, stepp3, true);});
    }

    public void createRide(View v){
        RideCreationNowDTO rideCreationDTO = new RideCreationNowDTO();

        PassengerIdEmailDTO passenger = new PassengerIdEmailDTO();
        passenger.setEmail("imenko@mail.com");
        passenger.setId(1);
        Set<PassengerIdEmailDTO> passengers = new HashSet<>();
        passengers.add(passenger);
        rideCreationDTO.setPassengers(passengers);

        rideCreationDTO.setBabyTransport(((CheckBox)v.findViewById(R.id.baby_checkbox)).isChecked());
        rideCreationDTO.setPetTransport(((CheckBox)v.findViewById(R.id.baby_checkbox)).isChecked());

        /*LocalDateTime now = LocalDateTime.now();
        TimePicker timePicker = v.findViewById(R.id.timePicker_passenger_stepper);
        LocalDateTime dateTime = LocalDateTime.of(now.getYear(), now.getMonth(),
                now.getDayOfMonth(), timePicker.getHour(), timePicker.getMinute());
        rideCreationDTO.setScheduledTime(dateTime);*/

        Set<RouteDTO> routeDTOS = new HashSet<>();
        LocationDTO start = new LocationDTO(45.25550453856233, 19.851038637778036);
        start.setAddress("Dunavska 4 Novi Sad");
        LocationDTO end = new LocationDTO(45.25701883039242, 19.845306955498636);
        end.setAddress("Laze Telečkog 16 Novi Sad");
        RouteDTO rute = new RouteDTO();
        rute.setDeparture(start);
        rute.setDestination(end);
        routeDTOS.add(rute);
        rideCreationDTO.setLocations(routeDTOS);

        if(((RadioButton)v.findViewById(R.id.standard_vehicle_radio_button)).isChecked()){
            rideCreationDTO.setVehicleType("STANDARD");
        }
        else if(((RadioButton)v.findViewById(R.id.luxury_vehicle_radio_button)).isChecked()){
            rideCreationDTO.setVehicleType("LUXURY");
        }
        else if(((RadioButton)v.findViewById(R.id.van_vehicle_radio_button)).isChecked()){
            rideCreationDTO.setVehicleType("VAN");
        }

        Call<ResponseBody> call = RestApiManager.restApiInterfacePassenger.createRide(rideCreationDTO);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.code() == 200){
                    Toast.makeText(getActivity().getApplicationContext(), "Vožnja uspešno napravljena!", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.d("REZ", t.getMessage() != null?t.getMessage():"error");
            }
        });
    }

    public void updateUI(boolean hide) {
        if(hide) {
            panic.setVisibility(View.INVISIBLE);
            inconsistency.setVisibility(View.INVISIBLE);
            rideInfoLabel.setVisibility(View.INVISIBLE);
            callDriver.setVisibility(View.INVISIBLE);
            messageDriver.setVisibility(View.INVISIBLE);
            timerLabel.setVisibility(View.INVISIBLE);
            timerHandler.removeCallbacks(timerRunnable);
            return;
        }
        panic.setVisibility(View.VISIBLE);
        inconsistency.setVisibility(View.VISIBLE);
        callDriver.setVisibility(View.VISIBLE);
        callDriver.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + currentRideDriver.getTelephoneNumber()));
            startActivity(intent);
        });
        messageDriver.setVisibility(View.VISIBLE);
        messageDriver.setOnClickListener(view -> {
            updateMessagesOverlay(false);
        });
        rideInfoLabel.setVisibility(View.VISIBLE);
        rideInfoLabel.setText("Procenjeno vreme (min): " +
                Integer.toString(currentRide.getEstimatedTimeInMinutes()) +
                "\nCena (din): " +
                String.format("%.2f",currentRide.getTotalCost()) +
                "\nVozač: " +
                currentRideDriver.getName() + " " + currentRideDriver.getSurname());
        timerLabel.setVisibility(View.VISIBLE);
        startTime = System.currentTimeMillis();
        timerHandler.postDelayed(timerRunnable,0);
    }

    public static void updatePanicOverlay(boolean hide) {
        if(hide){
            panicOverlay.setVisibility(View.INVISIBLE);
            updateAllButtons(false);
            return;
        }
        panicOverlay.setVisibility(View.VISIBLE);
        updateAllButtons(true);
    }

    public static void updateMessagesOverlay(boolean hide) {
        if(hide){
            messageOverlay.setVisibility(View.INVISIBLE);
            updateAllButtons(false);
            return;
        }
        messageOverlay.setVisibility(View.VISIBLE);
        updateAllButtons(true);
    }

    private static void updateAllButtons(boolean disable) {
        if(disable) {
            callDriver.setClickable(false);
            messageDriver.setClickable(false);
            panic.setClickable(false);
            inconsistency.setClickable(false);
        }
        else{
            callDriver.setClickable(true);
            messageDriver.setClickable(true);
            panic.setClickable(true);
            inconsistency.setClickable(true);
        }
    }


    private static void stepShow(RelativeLayout old, RelativeLayout newl, boolean left){
        int gravity;
        if(left){
            gravity = Gravity.RIGHT;
        }
        else{
            gravity = Gravity.LEFT;
        }
        Transition transition = new Slide(gravity);
        transition.setDuration(200);
        transition.addTarget(newl);
        TransitionManager.beginDelayedTransition(passenger_main_container, transition);
        old.setVisibility(View.INVISIBLE);
        newl.setVisibility(View.VISIBLE);
    }

    public static void toggleReservationFormVisibility(boolean hide) {
        if(hide) {
            stepp1.setVisibility(View.INVISIBLE);
            stepp2.setVisibility(View.INVISIBLE);
            stepp3.setVisibility(View.INVISIBLE);
            stepp4.setVisibility(View.INVISIBLE);
            stepp_add.setVisibility(View.INVISIBLE);
            return;
        }
        stepp1.setVisibility(View.VISIBLE);
        stepp2.setVisibility(View.VISIBLE);
        stepp3.setVisibility(View.VISIBLE);
        stepp4.setVisibility(View.VISIBLE);
        stepp_add.setVisibility(View.VISIBLE);
    }

}