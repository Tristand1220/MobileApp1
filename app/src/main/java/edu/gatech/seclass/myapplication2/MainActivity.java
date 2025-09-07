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

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import kotlinx.coroutines.ExecutorCoroutineDispatcher;
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
    private Button translate_btn;
    private TextView outputlabel, outputText;
    private MaterialCardView outputCard;

    private String selectedLangauge = "";
    private ExecutorService executorService;
    private OkHttpClient httpClient;

    //API related configurations
    private static final String SPANISH_CODE = "es";
    private static final String CHINESE_CODE = "zh-CN";
    private static final String JAPANESE_CODE = "ja";
    private static final String FRENCH_CODE = "fr";
    private static final String API_URL = "https://openl-translate.p.rapidapi.com/translate/bulk";
    private static final String API_KEY = "376d299e55msh833d1e58e318bebp1bd829jsn24d975c74503";
    private static final String API_HOST = "openl-translate.p.rapidapi.com";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Handle background task
        executorService = Executors.newSingleThreadExecutor();
        httpClient = new OkHttpClient();

        //Components and listeners
        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        languageGroup = findViewById(R.id.lang_group);
        inputField = findViewById(R.id.inputField);
        translate_btn = findViewById(R.id.translate_btn);
        outputlabel = findViewById(R.id.outputLabel);
        outputText = findViewById(R.id.outputtxt);
        outputCard = findViewById(R.id.outputCard);

        //Translate button should be disabled
        translate_btn.setEnabled(false);

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
    }

    private void handleLanguageSelection(int checkedId) {
        if (checkedId == R.id.spanish_radioButton) {
            selectedLangauge = SPANISH_CODE;
        } else if (checkedId == R.id.chz_radioButton) {
            selectedLangauge = CHINESE_CODE;
        } else if (checkedId == R.id.jap_radioButton) {
            selectedLangauge = JAPANESE_CODE;
        } else if (checkedId == R.id.fr_radioButton) {
            selectedLangauge = FRENCH_CODE;
        } else {
            selectedLangauge = "";
        }
    }

    //Updating translate button based on user's input
    private void updateTranslateButtonState() {
        String inputText = inputField.getText() != null ?
                inputField.getText().toString().trim() : "";
        boolean isText = !inputText.isEmpty();
        boolean haslanguage = !selectedLangauge.isEmpty();

        translate_btn.setEnabled(isText && haslanguage);
    }


    //Calling task to call API from OpenL
    private void performTranslation() {
        String textToTranslate = inputField.getText().toString().trim();

        if (textToTranslate.isEmpty() || selectedLangauge.isEmpty()) {
            Toast.makeText(this, "Please enter text and select a language", Toast.LENGTH_SHORT).show();
            return;
        }

        //Preforming  translation task
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String translatedText = translateText(textToTranslate, selectedLangauge);

                    //Update UI
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            hideLoading();
                            displayTranslation(translatedText);
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
