package edu.gatech.seclass.myapplication2;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONArray;
import org.json.JSONObject;





public class MainActivity extends AppCompatActivity {

    private RadioGroup languageGroup;
    private TextInputEditText inputField;
    private Button translate_btn, historyToggleBtn;
    private TextView outputlabel, outputText,historyEmptyMessage;
    private MaterialCardView outputCard;
    private RecyclerView historyRecycleView;

    private String selectedLanguage = "";
    private ExecutorService executorService;
    private OkHttpClient httpClient;

    private FirebaseFirestore db;
    private TranslationHistoryAdapter historyAdapter;
    private boolean isHistoryVisible = false;

    //API related configurations
    private static final String SPANISH_CODE = "es";
    private static final String CHINESE_CODE = "zh-CN";
    private static final String JAPANESE_CODE = "ja";
    private static final String API_URL = "https://openl-translate.p.rapidapi.com/translate/bulk";
    private static final String API_KEY = "KEY";
    private static final String API_HOST = "openl-translate.p.rapidapi.com";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Handle background task
        executorService = Executors.newSingleThreadExecutor();
        httpClient = new OkHttpClient();

        //Initialize firebase
        db = FirebaseFirestore.getInstance();
        historyAdapter = new TranslationHistoryAdapter();


        //Components and listeners
        initializeViews();
        setupListeners();

        loadTranslationHistory();
    }

    private void initializeViews() {
        languageGroup = findViewById(R.id.lang_group);
        inputField = findViewById(R.id.inputField);
        translate_btn = findViewById(R.id.translate_btn);
        historyToggleBtn = findViewById(R.id.historyToggleBtn);
        outputlabel = findViewById(R.id.outputLabel);
        outputText = findViewById(R.id.outputtxt);
        outputCard = findViewById(R.id.outputCard);
        historyRecycleView = findViewById(R.id.historyRecycleView);
        historyEmptyMessage = findViewById(R.id.historyEmptyMessage);

        //Translate button should be disabled
        translate_btn.setEnabled(false);

        //Recycler Views
        historyRecycleView.setLayoutManager(new LinearLayoutManager(this));
        historyRecycleView.setAdapter(historyAdapter);

        historyRecycleView.setVisibility(View.GONE);
        historyEmptyMessage.setVisibility(View.GONE);

        //lEFT OFF HERE

    }

    private void setupListeners() {
        // For radio buttons
        languageGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                handleLanguageSelection(i);
                updateTranslateButtonState();
            }
        });

        // For text input
        inputField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateTranslateButtonState();
                // Hides the previous translation once the user types again
                if (s.length() > 0 && outputCard.getVisibility() == View.VISIBLE) {
                    hideOutput();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        //Translate button listener event
        translate_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performTranslation();
            }
        });

        historyToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleHistory();
            }
        });
    }

    private void handleLanguageSelection(int checkedId) {
        if (checkedId == R.id.spanish_radioButton) {
            selectedLanguage = SPANISH_CODE;
        } else if (checkedId == R.id.chz_radioButton) {
            selectedLanguage = CHINESE_CODE;
        } else if (checkedId == R.id.jap_radioButton) {
            selectedLanguage = JAPANESE_CODE;
        } else {
            selectedLanguage = "";
        }
    }

    //Updating translate button based on user's input
    private void updateTranslateButtonState() {
        String inputText = inputField.getText() != null ?
                inputField.getText().toString().trim() : "";
        boolean isText = !inputText.isEmpty();
        boolean haslanguage = !selectedLanguage.isEmpty();

        translate_btn.setEnabled(isText && haslanguage);
    }


    //Calling task to call API from OpenL
    private void performTranslation() {
        String textToTranslate = inputField.getText().toString().trim();

        if (textToTranslate.isEmpty() || selectedLanguage.isEmpty()) {
            Toast.makeText(this, "Please enter text and select a language", Toast.LENGTH_SHORT).show();
            return;
        }

        //Preforming  translation task
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String translatedText = translateText(textToTranslate, selectedLanguage);

                    //Update UI
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            hideLoading();
                            displayTranslation(translatedText);

                            // Saving to firebase
                            saveToFirebase(textToTranslate,translatedText,selectedLanguage);
                        }
                    });

                } catch (Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            hideLoading();
                            showErrors("Translated failed: " + e.getMessage());
                        }
                    });
                }
            }
        });
    }

    //Actual API call to OpenL
    private String translateText(String text, String targetlang) throws Exception {
        try {
            //JSON request body
            JSONObject requestJson = new JSONObject();
            requestJson.put("target_lang", targetlang);

            //Text array with text item
            JSONArray textArray = new JSONArray();
            textArray.put(text);
            requestJson.put("text", textArray);

            //API Request body
            MediaType mediaType = MediaType.parse("application/json");
            RequestBody body = RequestBody.create(mediaType, requestJson.toString());

            Request request = new Request.Builder()
                    .url(API_URL)
                    .post(body)
                    .addHeader("x-rapidapi-key", API_KEY)
                    .addHeader("x-rapidapi-host", API_HOST)
                    .addHeader("Content-Type", "application/json")
                    .build();

            Response response = httpClient.newCall(request).execute();

            if (!response.isSuccessful()) {
                throw new Exception("Api call failed. Code: " + response.code() + " - " + response.message());
            }

            //Parsing response for translated text
            String responseBody = response.body().string();
            JSONObject responseJson = new JSONObject(responseBody);

            if (responseJson.has("translatedTexts")) {
                JSONArray translations = responseJson.getJSONArray("translatedTexts");
                if (translations.length() > 0) {
                    String first = translations.getString(0);
                    return first;
                }
            }
            throw new Exception("Unexpected response format");
        } catch(Exception e) {
            throw new Exception("Translation failed: " + e.getMessage());
        }
    }


    // Display translated text
    private void showLoading(){
        translate_btn.setEnabled(false);
        translate_btn.setText("Translating...");
    }

    private void hideLoading(){
        translate_btn.setEnabled(true);
        translate_btn.setText("Translate");
    }

    // Display translated text
    private void displayTranslation(String translatedText){
        outputText.setText(translatedText);
        outputlabel.setVisibility(View.VISIBLE);
        outputCard.setVisibility(View.VISIBLE);
    }

    //Hiding output components
    private void hideOutput(){
        outputlabel.setVisibility(View.GONE);
        outputCard.setVisibility(View.GONE);
    }

    //Toast error messages
    private void showErrors(String errorMsg){
        Toast.makeText(this,errorMsg, Toast.LENGTH_LONG).show();
    }
    //Saving translations to firebase
    private void saveToFirebase(String originalText, String translatedText, String targetLang){
        TranslationHistory history = new TranslationHistory(originalText, translatedText, "en", targetLang);

         db.collection("translations")
                .add(history)
                .addOnSuccessListener(documentReference -> {
                    history.setDocumentID(documentReference.getId());
                    historyAdapter.addTranslation(history);
                    updateHistoryEmptyState();
                })
                .addOnFailureListener(e -> {
                    //Use to log for debugging failures
                });
    }

    //Loading Translation history from firebase db
    private void loadTranslationHistory(){
        db.collection("translations")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(25) // Only loading the last 25 translations
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<TranslationHistory> historyList = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots){
                        TranslationHistory history =document.toObject(TranslationHistory.class);
                        history.setDocumentID(document.getId());
                        historyList.add(history);
                    }
                    historyAdapter.updateHistoryList(historyList);
                    updateHistoryEmptyState();
                })
                .addOnFailureListener(e -> {
                    updateHistoryEmptyState();
                });
    }

    //Toggles for history list
    private void toggleHistory(){
        isHistoryVisible = !isHistoryVisible;

        if (isHistoryVisible){
            historyRecycleView.setVisibility(View.VISIBLE);
            historyToggleBtn.setText("Hide History");
            updateHistoryEmptyState();
            hideOutput();
        } else {
            historyRecycleView.setVisibility(View.GONE);
            historyEmptyMessage.setVisibility(View.GONE);
            historyToggleBtn.setText("Show History");

        }
    }

    // Updating history empty state
    private void updateHistoryEmptyState(){
        if (isHistoryVisible){
            if(historyAdapter.getItemCount() == 0){
                historyRecycleView.setVisibility(View.VISIBLE);
                historyEmptyMessage.setVisibility(View.GONE);
            }else{
                historyRecycleView.setVisibility(View.VISIBLE);
                historyEmptyMessage.setVisibility(View.GONE);
            }
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        if(executorService != null && !executorService.isShutdown()){
            executorService.shutdown();
        }
        //Cleans http client
        if (httpClient != null){
            httpClient.dispatcher().executorService().shutdown();
        }
    }
}
