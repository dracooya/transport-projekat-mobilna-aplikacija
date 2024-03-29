package com.example.uberapp_tim9.driver.fragments;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uberapp_tim9.R;
import com.example.uberapp_tim9.driver.DriverMainActivity;
import com.example.uberapp_tim9.driver.notificationManager.NotificationActionReceiver;
import com.example.uberapp_tim9.driver.notificationManager.NotificationService;
import com.example.uberapp_tim9.driver.ride_history.DriverInRidePassengersData;
import com.example.uberapp_tim9.driver.ride_history.adapters.DriverInRidePassengersAdapter;
import com.example.uberapp_tim9.map.MapInit;
import com.example.uberapp_tim9.model.Message;
import com.example.uberapp_tim9.model.MessageType;
import com.example.uberapp_tim9.model.Ride;
import com.example.uberapp_tim9.model.User;
import com.example.uberapp_tim9.model.dtos.MessageDTO;
import com.example.uberapp_tim9.model.dtos.MessageSimpleDTO;
import com.example.uberapp_tim9.model.dtos.PassengerIdEmailDTO;
import com.example.uberapp_tim9.model.dtos.PassengerWithoutIdPasswordDTO;
import com.example.uberapp_tim9.model.dtos.RejectionReasonDTO;
import com.example.uberapp_tim9.model.dtos.RideCreatedDTO;
import com.example.uberapp_tim9.model.dtos.RouteDTO;
import com.example.uberapp_tim9.model.dtos.TimeUntilOnDepartureDTO;
import com.example.uberapp_tim9.model.dtos.VehicleDTO;
import com.example.uberapp_tim9.passenger.PassengerMainActivity;
import com.example.uberapp_tim9.passenger.adapters.MessagesListAdapter;
import com.example.uberapp_tim9.passenger.fragments.MapFragment;
import com.example.uberapp_tim9.shared.LoggedUserInfo;
import com.example.uberapp_tim9.shared.directions.RouteDrawer;
import com.example.uberapp_tim9.shared.rest.RestApiManager;
import com.example.uberapp_tim9.shared.sockets.SocketsConfiguration;
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
import java.util.ArrayList;
import java.util.List;

import io.reactivex.disposables.Disposable;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class DriverMainFragment extends Fragment {

    public static final SocketsConfiguration socketsConfiguration = new SocketsConfiguration();
    private static Button startRide;
    private static TextView routeLabel;
    public static RideCreatedDTO acceptedRide;
    public static boolean rideHasStarted = false;
    private static Context context;
    private static Button messageSend;
    private static FrameLayout messageOverlay;
    private static Button closeMessageOverlay;
    private static TextInputEditText msg;
    private static int msg_id = 4;
    private final Gson gson = new GsonBuilder().registerTypeAdapter(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
        @Override
        public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            return LocalDateTime.parse(json.getAsString());
        }
    }).create();

    public static TextView timer;
    public static Button panicButton;
    public static Button endRideButton;

    private Marker driverMarker;

    private static FrameLayout panicOverlay;
    private static Button closeOverlay;
    private static Button panicSend;

    public DriverMainFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Subscribe to events from websocket (ride is ordered)
        Disposable subscription = socketsConfiguration.stompClient.topic("/ride-ordered/get-ride").subscribe(message ->
        {
            RideCreatedDTO retrieved = gson.fromJson(message.getPayload(), new TypeToken<RideCreatedDTO>(){}.getType());
            NotificationActionReceiver.RIDE_ID = Integer.toString(retrieved.getId());
            acceptedRide = retrieved;
            NotificationService.createRideAcceptanceNotification(getActivity(),retrieved.getRideSummary(), DriverMainActivity.CHANNEL_ID);
            },
        throwable -> Log.e(TAG, throwable.getMessage()));
        MapInit mapFunctionalities = new MapInit();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_driver_home, container, false);
        FragmentManager fm = getChildFragmentManager();
        MapFragment mapFragment = new MapFragment(true);
        FragmentTransaction fragmentTransaction = fm.beginTransaction();
        fragmentTransaction.add(R.id.map_container_driver, mapFragment);
        fragmentTransaction.commit();
        fm.executePendingTransactions();
        startRide = v.findViewById(R.id.start_ride);
        timer = v.findViewById(R.id.timerCard);
        panicButton = v.findViewById(R.id.panicButton);
        endRideButton = v.findViewById(R.id.endRideButton);
        context = getActivity();

        panicOverlay = v.findViewById(R.id.panic_overlay);
        closeOverlay = v.findViewById(R.id.close_button);
        panicSend = v.findViewById(R.id.panic_message_send_button);

        DriverInRidePassengersAdapter adapter = new DriverInRidePassengersAdapter();
        RecyclerView list = v.findViewById(R.id.passengers_list);
        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        list.setLayoutManager(llm);
        list.setAdapter(adapter);

        panicButton.setOnClickListener(view -> {
            updatePanicOverlay(false);
//            Toast.makeText(context, "Support služba je obaveštena.", Toast.LENGTH_SHORT).show();
//            hidePanicButton();
        });

        panicSend.setOnClickListener(view -> {
            TextInputEditText reason = v.findViewById(R.id.panic_message);
            String reasonText = reason.getText().toString().trim();
            if(reasonText.length() == 0) {
                reason.setError(getString(R.string.zeroLengthError));
            } else {
                String rideId = Integer.toString(acceptedRide.getId());
                RejectionReasonDTO reasonDTO = new RejectionReasonDTO(reasonText);
                Call<ResponseBody> call = RestApiManager.restApiInterfacePassenger.panic(rideId, reasonDTO);
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
                        panicButton.setVisibility(View.GONE);
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        Log.d("REZ", t.getMessage() != null?t.getMessage():"error");
                    }
                });
            }
        });

        endRideButton.setOnClickListener(view -> {
            Call<ResponseBody> call = RestApiManager.restApiInterfaceDriver.endRide(acceptedRide.getId().toString());
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if(response.code() == 200){
                        Toast.makeText(context, "Vožnja je završena.", Toast.LENGTH_SHORT).show();
                        hideTimer();
                        hidePanicButton();
                        hideEndRideButton();
                        rideHasStarted = false;
                        acceptedRide = null;
                        NotificationActionReceiver.RIDE_ID = "";
                        NotificationActionReceiver.currentRide = null;
                        NotificationActionReceiver.currentVehicle = null;
                        updateUI(true);
                        driverMarker.setIcon(MapFragment.BitmapFromVector(getActivity(), R.drawable.greencar));
                        DriverInRidePassengersData.passengers = new ArrayList<>();
                        adapter.notifyDataSetChanged();
                        for (Polyline polyline : MapFragment.polylines) {
                            polyline.remove();
                        }
                        RouteDrawer.clearPolyLine();
                    } else if (response.code() == 400){
                        Toast.makeText(getActivity(), "Ne možete završiti vožnju koja nije završena.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getActivity(), "Vožnja ne postoji!", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Log.d("REZ", t.getMessage() != null?t.getMessage():"error");
                }
            });
        });

        startRide.setOnClickListener(view -> {
            Call<ResponseBody> call = RestApiManager.restApiInterfaceDriver.startRide(acceptedRide.getId().toString());
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.code() == 200) {
                        Toast.makeText(getActivity(), "Vožnja uspešno startovana!", Toast.LENGTH_SHORT).show();
                        rideHasStarted = true;
                        driverMarker = MapFragment.driversMarkers.get(acceptedRide.getDriver().getId());
                        driverMarker.setIcon(MapFragment.BitmapFromVector(getActivity(), R.drawable.redcar));
                        updateUI(true);

                        final LatLng[] departure = new LatLng[1];
                        final LatLng[] destination = new LatLng[1];

                        VehicleDTO currentVehicle = NotificationActionReceiver.currentVehicle;

                        departure[0] = new LatLng(currentVehicle.getCurrentLocation().getLatitude(),
                                currentVehicle.getCurrentLocation().getLongitude());
                        for (RouteDTO route : NotificationActionReceiver.currentRide.getLocations()) {
                            destination[0] = new LatLng(route.getDestination().getLatitude(), route.getDestination().getLongitude());
                            break;
                        }

                        List<Integer> passengersToPing = new ArrayList<>();
                        for (PassengerIdEmailDTO passenger : DriverMainFragment.acceptedRide.getPassengers()) {
                            passengersToPing.add(passenger.getId());
                        }

                        MapInit init = new MapInit();
                        init.simulateRoute(departure[0],
                                destination[0],
                                driverMarker,
                                false,
                                false,
                                currentVehicle.getId(),
                                true,
                                600,
                                false,
                                null);
                        displayTimer();
                        displayPanicButton();

                        Call<ResponseBody> passengersCall = RestApiManager.restApiInterfaceDriver.getPassengers(acceptedRide.getId().toString());
                        passengersCall.enqueue(new Callback<ResponseBody>() {
                            @Override
                            public void onResponse(Call<ResponseBody> passengersCall, Response<ResponseBody> passengersResponse) {
                                if (passengersResponse.code() == 200) {
                                    try {
                                        List<PassengerWithoutIdPasswordDTO> passengers = new Gson().fromJson(passengersResponse.body().string(), new TypeToken<List<PassengerWithoutIdPasswordDTO>>(){}.getType());
                                        DriverInRidePassengersData.passengers = passengers;
                                        adapter.notifyDataSetChanged();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }

                            @Override
                            public void onFailure(Call<ResponseBody> passengersCall, Throwable passengersT) {
                                Log.d("REZ", passengersT.getMessage() != null?passengersT.getMessage():"error");
                            }
                        });
                    }
                    else if (response.code() == 400){
                        Toast.makeText(getActivity(), "Ne možete prihvatiti vožnju koja nema status 'Prihvaćena'!", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Toast.makeText(getActivity(), "Vožnja ne postoji!", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Log.d("REZ", t.getMessage() != null?t.getMessage():"error");
                }
            });
        });
        routeLabel = v.findViewById(R.id.route_label);
        MessagesListAdapter messagesListAdapter = new MessagesListAdapter(LoggedUserInfo.id);
        RecyclerView messagesList = v.findViewById(R.id.messages_list);
        LinearLayoutManager messagesLlm = new LinearLayoutManager(getActivity());
        messagesList.setLayoutManager(messagesLlm);
        messagesList.setAdapter(messagesListAdapter);
        messageSend = v.findViewById(R.id.message_send_button);
        messageOverlay = v.findViewById(R.id.message_overlay);
        closeMessageOverlay = v.findViewById(R.id.close_message_overlay_button);
        closeMessageOverlay.setOnClickListener(view -> {
            updateMessagesOverlay(true);
            panicButton.setClickable(true);
        });
        msg = v.findViewById(R.id.email_input);
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
                    1,
                    msg.getText().toString(),
                    LocalDateTime.now(),
                    MessageType.VOZNJA,
                    acceptedRide.getId());
            PassengerMainActivity.socketsConfiguration.stompClient.send("/topic/message",PassengerMainActivity.gson.toJson(message_new)).subscribe();
        });
        return v;
    }

    private void updatePanicOverlay(boolean hide) {
        if (hide) {
            panicOverlay.setVisibility(View.GONE);
            return;
        }
        panicOverlay.setVisibility(View.VISIBLE);
    }

    public static void sendOnLocationNotification(String rideId) {
        socketsConfiguration.stompClient.send("/topic/on-location",rideId).subscribe();
    }

    public static void sendLocationUpdatesNotification(TimeUntilOnDepartureDTO dto) {
        socketsConfiguration.stompClient.send("/topic/location-tracker",new Gson().toJson(dto)).subscribe();
    }

    public static void updateUI(boolean hide) {
        if(hide) {
            startRide.setVisibility(View.INVISIBLE);
            routeLabel.setVisibility(View.INVISIBLE);
        } else {
            startRide.setVisibility(View.VISIBLE);
            for (RouteDTO route : acceptedRide.getLocations()) {
                routeLabel.setText(route.getDeparture().getAddress() + " - " + route.getDestination().getAddress());
                break;
            }
            routeLabel.setVisibility(View.VISIBLE);
        }
    }

    public static void displayTimer() {
        timer.setVisibility(View.VISIBLE);
    }

    public static void hideTimer() {
        timer.setVisibility(View.INVISIBLE);
    }

    public static void displayPanicButton() {
        panicButton.setVisibility(View.VISIBLE);
    }

    public static void hidePanicButton() {
        panicButton.setVisibility(View.INVISIBLE);
    }

    public static void displayEndRideButton() {
        endRideButton.setVisibility(View.VISIBLE);
    }

    public static void hideEndRideButton() {
        endRideButton.setVisibility(View.INVISIBLE);
    }

    public static void updateTimer(int time) {
        Log.d("REZ", "updateTimer: " + time);
        if (time > 0) {
            timer.setText(String.format("%ss do destinacije", String.valueOf(time)));
        } else {
            timer.setText("Stigli ste na destinaciju!");
        }
    }

    public static void cancelAfter5Minutes(String rideId) {
        if(!rideHasStarted) {
            RejectionReasonDTO reasonDTO = new RejectionReasonDTO("Putnici se nisu pojavili na lokaciji nakon 5 minuta.");
            Call<ResponseBody> call = RestApiManager.restApiInterfaceDriver.denyRide(rideId,reasonDTO);
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.code() == 200){
                        Toast.makeText(context, "Vožnja se odbija (putnici nisu na lokaciji nakon 5 minuta).", Toast.LENGTH_LONG).show();
                        RouteDrawer.clearPolyLine();
                        updateUI(true);
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Log.d("REZ", t.getMessage() != null?t.getMessage():"error");
                }
            });
        }
    }

    public static void updateMessagesOverlay(boolean hide) {
        if(hide){
            messageOverlay.setVisibility(View.INVISIBLE);
            return;
        }
        messageOverlay.setVisibility(View.VISIBLE);
    }
}