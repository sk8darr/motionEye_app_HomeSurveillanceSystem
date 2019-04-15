//Resolve Call Numbers
package com.jairaj.janglegmail.motioneye;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt;
import uk.co.samuelwall.materialtaptargetprompt.extras.backgrounds.RectanglePromptBackground;
import uk.co.samuelwall.materialtaptargetprompt.extras.focals.RectanglePromptFocal;

public class AddCam extends AppCompatActivity
{
    DataBase myDb;
    private ListView dList; //For displaying device list
    boolean checked = false; //Flag to store state of ListView dList' items: checked or not checked
    Toolbar toolbar; //Tool bar holding ToolBar title and edit, delete buttons and about option
    MenuItem dummyDelete; //for storing layout item of delete button in toolbar
    MenuItem dummyEdit; //for storing layout item of edit button in toolbar
    MenuItem dummyAbout; //for storing layout item of about option in toolbar
    //private AdView mAdView; //for storing layout item of adview
    //AdRequest adRequest; //for storing ad request to adUnit id in linked layout file
    //AdListener adListener; //Listener for ads
    short isFirstTimeDriveV = 0; //0 = never appeared before; 1 = First Time; 2 = not First Time
    int targetForDriveIcon = 0;

    FloatingActionButton fab; //object storing id of FAB in linked layout xml
    // Create a HashMap List from String Array elements
    List<HashMap<String, String>> listItems = new ArrayList<>(); //HashMap inside list
    // Create an ArrayAdapter from List
    SimpleAdapter adapter; //Adapter to link List with ListView
    HashMap<String, String> labelUrlPort = new HashMap<>(); //HashMap to store Label, Url + Port

    private static final int REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE);

        myDb = new DataBase(this);

        myDb.insertNewColumn();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add__cam);

        fab = findViewById(R.id.fab);
        toolbar = findViewById(R.id.toolbar);
        adapter = new SimpleAdapter(this, listItems, R.layout.custom_list_item,
                new String[]{"First Line", "Second Line"},
                new int[]{R.id.title_label_text, R.id.subtitle_url_port_text});

        if (dList == null)
            dList = findViewById(R.id.device_list);


        setSupportActionBar(toolbar);
        toolbar.setTitle(R.string.Camera_List);

        if(isFirstTime())
        {
            displayTutorial(1);
        }

        fab.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                gotoAddDeviceDetail("0");
            }
        });

        fetchData();
        // Add this Runnable
        dList.post(new Runnable() {
            @Override
            public void run()
            {
                final Handler handler =  new Handler()
                {
                    @Override
                    public void handleMessage(Message msg)
                    {
                        showHideDriveButton();
                        showHidePrev();
                    }
                };

                Thread t = new Thread() {
                    @Override
                    public void run() {
                        handler.sendEmptyMessage(0);
                    }
                };

                t.run();
            }
        });

        dList.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                if (!checked)
                {
                    String selectedUrlPort = ((TextView) view.findViewById(R.id.subtitle_url_port_text)).getText().toString();
                    goToWebMotionEye(selectedUrlPort, Constants.MODE_CAMERA);
                }
                else
                {
                    CheckBox cb = view.findViewById(R.id.checkBox);
                    cb.setChecked(!cb.isChecked());

                    int f = getItemCheckedCountInDList();

                    if (f == 0)
                    {
                        for (int i = 0; i < dList.getChildCount(); i++)
                        {
                            view = dList.getChildAt(i);
                            cb = view.findViewById(R.id.checkBox);
                            cb.setVisibility(View.GONE);
                        }
                        toggleActionBarElements();
                    }
                }
            }
        });

        dList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener()
        {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
            {
                if(!checked)
                {
                    int i = 0;
                    while (i < dList.getChildCount()) {
                        view = dList.getChildAt(i);
                        CheckBox cb = view.findViewById(R.id.checkBox);
                        cb.setVisibility(View.VISIBLE);
                        if (i == position)
                            cb.setChecked(true);
                        i++;
                    }
                    toggleActionBarElements();
                }
                else
                {
                    int i = 0;
                    while (i < dList.getChildCount()) {
                        view = dList.getChildAt(i);
                        CheckBox cb = view.findViewById(R.id.checkBox);
                        cb.setVisibility(View.GONE);
                        if (i == position)
                            cb.setChecked(false);
                        i++;
                    }
                    toggleActionBarElements();
                }
                return true;
            }
        });

        if(listItems.size() == 1){
            String url = listItems.get(0).get("Second Line");
            int mode = TextUtils.isEmpty(
                    myDb.getDrive_from_Label(listItems.get(0).get("First Line")))?
                    Constants.MODE_CAMERA: Constants.MODE_DRIVE;
            goToWebMotionEye(url, mode);
        }
    }

    private void goToWebMotionEye(String urlPort, @Constants.ServerMode int mode){
        Bundle bundle = new Bundle();
        //Add your data from getFactualResults method to bundle
        bundle.putString(Constants.KEY_URL_PORT, urlPort);
        bundle.putInt(Constants.KEY_MODE, mode);
        Intent i = new Intent(AddCam.this, web_motion_eye.class);
        i.putExtras(bundle);
        startActivity(i);
    }

    private void fetchData()
    {
        String url; //For storing url extracted from SQL
        String port; //For storing port extracted from SQL
        String label; //For storing label extracted from SQL
        String urlPort; //For storing url:port merged

        labelUrlPort.clear();
        Cursor res = myDb.getAllData();
        if (res.getCount() != 0)
        {
            while (res.moveToNext())
            {
                label = res.getString(1);
                url = res.getString(2);
                port = res.getString(3);

                if (!port.equals(""))
                    urlPort = url + ":" + port;
                else
                    urlPort = url;

                labelUrlPort.put(label, urlPort);
            }
        }

        final Handler handler =  new Handler()
        {
            @Override
            public void handleMessage(Message msg)
            {
                addToList();
            }
        };

        Thread t = new Thread() {
            @Override
            public void run() {
                handler.sendEmptyMessage(0);
            }
        };

        t.run();

        res.close();
    }

    private void addToList()
    {
        dList.setAdapter(null);
        listItems.clear();

        for (Object o : labelUrlPort.entrySet())
        {
            HashMap<String, String> resultsMap = new HashMap<>();
            Map.Entry pair = (Map.Entry) o;
            resultsMap.put("First Line", pair.getKey().toString());
            resultsMap.put("Second Line", pair.getValue().toString());
            listItems.add(resultsMap);
        }
        dList.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    private void gotoAddDeviceDetail(String editMode)
    {
        String deleteLabel = "";
        Bundle bundle = new Bundle();

        if(editMode.equals("1"))
        {
            int i = 0;
            while (i < dList.getChildCount())
            {
                View view = dList.getChildAt(i);
                CheckBox cb = view.findViewById(R.id.checkBox);

                if (cb.isChecked())
                {
                    deleteLabel = ((TextView) view.findViewById(R.id.title_label_text)).getText().toString();
                    cb.setChecked(false);
                }
                cb.setVisibility(View.GONE);
                i++;
            }
            checked = false;

            toggleActionBarElements();
            bundle.putString("LABEL", deleteLabel);
        }

        bundle.putString("EDIT", editMode);

        Intent intentForAddDevice = new Intent(AddCam.this, add_device_detail.class);
        //Add the bundle to the intent
        intentForAddDevice.putExtras(bundle);
        startActivityForResult(intentForAddDevice, 0);
    }

    @Override
    protected void onActivityResult(int requestCode, final int resultCode, Intent data)
    {
        if(dummyDelete != null)
            dummyDelete.setVisible(false);
        if(dummyEdit != null)
            dummyEdit.setVisible(false);
        if(dummyAbout != null)
            dummyAbout.setVisible(true);

        toolbar.setTitle(R.string.Camera_List);

        fab.show();

        checked = false;
        fetchData();

        dList.post(new Runnable() {
            @Override
            public void run()
            {
                final Handler handler =  new Handler()
                {
                    @Override
                    public void handleMessage(Message msg)
                    {
                        showHideDriveButton();
                        showHidePrev();
                    }
                };

                Thread t = new Thread() {
                    @Override
                    public void run() {
                        handler.sendEmptyMessage(0);
                    }
                };

                t.run();

                if(resultCode != 2)
                {
                    boolean shitB;
                    String shitS = "";
                    int shitI;

                    shitB = isFirstTimeDevice();
                    shitI = isFirstTimeDriveV;

                    shitS = "Boolean is: " + Boolean.toString(shitB) + " Integer is: " + Integer.toString(shitI);

                    if(shitB && (isFirstTimeDriveV == 0))
                        displayTutorial(2);
                    else if(!shitB && (isFirstTimeDriveV == 1))
                        displayTutorial(3);
                    else if(shitB && (isFirstTimeDriveV == 1))
                        displayTutorial(4);
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_add__cam, menu);
        dummyDelete = menu.findItem(R.id.delete);
        dummyEdit = menu.findItem(R.id.edit);
        dummyAbout = menu.findItem(R.id.action_about);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement

        if (id == R.id.delete && getItemCheckedCountInDList() > 0 && checked){
            int i = 0;
            while (i < dList.getChildCount()) {
                View view = dList.getChildAt(i);
                CheckBox cb = view.findViewById(R.id.checkBox);

                if (cb.isChecked()) {
                    String delLabel = ((TextView) view.findViewById(R.id.title_label_text)).getText().toString();
                    deleteData(delLabel);
                    cb.setChecked(false);
                }
                i++;
            }
            fetchData();
            toggleActionBarElements();
        }


        if(id == R.id.edit && checked) {
            int f = getItemCheckedCountInDList();

            if (f > 1) {
                Toast.makeText(getBaseContext(), "Select only one entry to edit", Toast.LENGTH_SHORT).show();
            }
            else
                gotoAddDeviceDetail("1");
        }

        if(id == R.id.action_about) {
            Intent intentAboutPage = new Intent(AddCam.this, About_Page.class);
            startActivity(intentAboutPage);
        }
        return super.onOptionsItemSelected(item);
    }

    private void deleteData(String delLabel)
    {
        Integer deletedRows = myDb.deleteData(delLabel);
        if (deletedRows <= 0)
            Toast.makeText(AddCam.this, "Failed to delete", Toast.LENGTH_LONG).show();
    }

    public void onDriveIconClick(View v)
    {
        //get the row the clicked button is in
        ConstraintLayout vwParentRow = (ConstraintLayout)v.getParent();
        TextView labelViewAtDriveIcClick = vwParentRow.findViewById(R.id.title_label_text);
        String labelTextAtDriveIcClick = labelViewAtDriveIcClick.getText().toString();
        String driveLink = myDb.getDrive_from_Label(labelTextAtDriveIcClick);

        goToWebMotionEye(driveLink, Constants.MODE_DRIVE);
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void onExpandCamClick(View v)
    {
        //get the row the clicked button is in
        ConstraintLayout vwParentRow = (ConstraintLayout)v.getParent();
        WebView previewView = vwParentRow.findViewById(R.id.preview_webview);

        TextView labelViewAtExpandIcClick = vwParentRow.findViewById(R.id.title_label_text);
        String labelTextAtExpandIcClick = labelViewAtExpandIcClick.getText().toString();

        ImageView expandButton = vwParentRow.findViewById(R.id.expand_button);

        if(previewView.getVisibility() == View.GONE)
        {
            String urlLink = myDb.getUrl_from_Label(labelTextAtExpandIcClick);

            expandButton.setImageResource(R.drawable.collapse_button);

            previewView.setVisibility(View.VISIBLE);

            previewView.getSettings().setJavaScriptEnabled(true);
            previewView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            previewView.setWebViewClient(new WebViewClient());
            previewView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
            previewView.loadUrl(urlLink);
            previewView.setInitialScale(100);

            boolean isUpdate = myDb.updatePrevStat(labelTextAtExpandIcClick, "1");
            if(!isUpdate)
                Toast.makeText(AddCam.this, R.string.error_try_delete,Toast.LENGTH_LONG).show();
        }

        else
        {
            boolean isUpdate = myDb.updatePrevStat(labelTextAtExpandIcClick, "0");
            if(!isUpdate)
                Toast.makeText(AddCam.this, R.string.error_try_delete,Toast.LENGTH_LONG).show();

            expandButton.setImageResource(R.drawable.expand_down);
            previewView.loadUrl("about:blank");
            previewView.setVisibility(View.GONE);
        }
    }

    private void displayTutorial(int callNumber)
    {
        /* callNumber usage
         * 1 = First Time App Opened
         * 2 = First Time Device added
         * 3 = Not First Time for Device addition but First Time for Drive
         * 4 = First Time for device addition as well as drive
         */
        if(callNumber == 1)
        {
            new MaterialTapTargetPrompt.Builder(AddCam.this)
                    .setTarget(R.id.fab)
                    .setPrimaryText(R.string.tut_title_add_button)
                    .setSecondaryText(R.string.tut_sub_add_button)
                    .setBackgroundColour(Color.argb(255, 30, 90, 136))
                    .show();
        }

        else if(callNumber == 2)
        {
            new MaterialTapTargetPrompt.Builder(AddCam.this)
                    .setTarget(R.id.dummy_show_case_button)
                    .setFocalColour(Color.argb(0, 0, 0, 0))
                    .setPrimaryText(R.string.tut_title_device_list)
                    .setSecondaryText(R.string.tut_sub_device_list)
                    .setBackgroundColour(Color.argb(255, 30, 90, 136))
                    .setPromptBackground(new RectanglePromptBackground())
                    .setPromptFocal(new RectanglePromptFocal())
                    .setPromptStateChangeListener(new MaterialTapTargetPrompt.PromptStateChangeListener()
                    {
                        @Override
                        public void onPromptStateChanged(MaterialTapTargetPrompt prompt, int state)
                        {
                            if (state == MaterialTapTargetPrompt.STATE_FOCAL_PRESSED)
                            {
                                Handler handler = new Handler();
                                handler.postDelayed(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        displayTutorial(3);
                                    }
                                }, 800); //delay
                            }
                            if (state == MaterialTapTargetPrompt.STATE_DISMISSED)
                            {
                                Handler handler = new Handler();
                                handler.postDelayed(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        displayTutorial(3);
                                    }

                                }, 1000); //delay
                            }
                        }
                    })
                    .show();
        }

        else if(callNumber == 3)
        {
            new MaterialTapTargetPrompt.Builder(AddCam.this)
                    .setTarget(targetForDriveIcon)
                    .setPrimaryText(R.string.tut_title_drive_icon)
                    .setSecondaryText(R.string.tut_sub_drive_icon)
                    .setBackgroundColour(Color.argb(255, 30, 90, 136))
                    .show();
        }

        else if(callNumber == 4)
        {
            displayTutorial(2);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void showHidePrev()
    {
        View view;
        int i = 0;
        while (i < dList.getChildCount())
        {
            view = dList.getChildAt(i);
            TextView eachLabel = view.findViewById(R.id.title_label_text);
            String eachLabelText = eachLabel.getText().toString();
            String prev = myDb.getPrevStat_from_Label(eachLabelText);

            WebView previewView = view.findViewById(R.id.preview_webview);

            String labelTextAtExpandIcClick = eachLabel.getText().toString();

            ImageView expandButton = view.findViewById(R.id.expand_button);

            if(prev.equals("1"))
            {
                String urlLink = myDb.getUrl_from_Label(labelTextAtExpandIcClick);

                expandButton.setImageResource(R.drawable.collapse_button);

                previewView.setVisibility(View.VISIBLE);

                previewView.getSettings().setJavaScriptEnabled(true);
                previewView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                previewView.setWebViewClient(new WebViewClient());
                previewView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
                previewView.loadUrl(urlLink);
                previewView.setInitialScale(100);

                boolean isUpdate = myDb.updatePrevStat(labelTextAtExpandIcClick, "1");
                if(!isUpdate)
                    Toast.makeText(AddCam.this, R.string.error_try_delete,Toast.LENGTH_LONG).show();
            }

            else
            {
                boolean isUpdate = myDb.updatePrevStat(labelTextAtExpandIcClick, "0");
                if(!isUpdate)
                    Toast.makeText(AddCam.this, R.string.error_try_delete,Toast.LENGTH_LONG).show();

                expandButton.setImageResource(R.drawable.expand_down);
                previewView.loadUrl("about:blank");
                previewView.setVisibility(View.GONE);
            }
            i++;
        }
    }

    private void showHideDriveButton()
    {
        View view;
        int i = 0;
        while (i < dList.getChildCount()) {
            view = dList.getChildAt(i);
            TextView eachLabel = view.findViewById(R.id.title_label_text);
            String eachLabelText = eachLabel.getText().toString();
            String driveLink = myDb.getDrive_from_Label(eachLabelText);
            ImageButton driveButton = view.findViewById(R.id.button_drive);

            if (driveLink.equals(""))
                driveButton.setVisibility(View.GONE);
            else {
                targetForDriveIcon = R.id.button_drive;
                driveButton.setVisibility(View.VISIBLE);
                if (isFirstTimeDrive())
                    isFirstTimeDriveV = 1;
                else
                    isFirstTimeDriveV = 2;
            }
            i++;
        }
    }

    private int getItemCheckedCountInDList()
    {
        View view;
        CheckBox cb;
        int f = 0;
        for (int i = 0; i < dList.getChildCount(); i++)
        {
            view = dList.getChildAt(i);
            cb = view.findViewById(R.id.checkBox);
            if (cb.isChecked())
                f++;
        }
        return f;
    }

    private void toggleActionBarElements()
    {
        dummyAbout.setVisible(!dummyAbout.isVisible());
        dummyDelete.setVisible(!dummyDelete.isVisible());
        dummyEdit.setVisible(!dummyEdit.isVisible());

        if(toolbar.getTitle().equals(""))
            toolbar.setTitle(R.string.Camera_List);
        else
            toolbar.setTitle("");

        if(fab.getVisibility() == View.GONE)
            fab.show();
        else
            fab.hide();

        checked = !checked;
    }

    private boolean isFirstTime()
    {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        boolean ranBefore = preferences.getBoolean("RanBefore", false);
        if (!ranBefore) {
            // first time
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("RanBefore", true);
            editor.apply();
        }
        return !ranBefore;
    }

    private boolean isFirstTimeDevice()
    {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        boolean ranBefore = preferences.getBoolean("Device_added_before", false);
        if (!ranBefore) {
            // first time
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("Device_added_before", true);
            editor.apply();
        }
        return !ranBefore;
    }

    private boolean isFirstTimeDrive()
    {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        boolean ranBefore = preferences.getBoolean("Drive_RanBefore", false);
        if (!ranBefore)
        {
            // first time
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("Drive_RanBefore", true);
            editor.apply();
        }
        return !ranBefore;
    }

    @Override
    public void onBackPressed()
    {
        int f = getItemCheckedCountInDList();
        if(f != 0)
        {
            View view;
            CheckBox cb;
            for (int i = 0; i < dList.getChildCount(); i++)
            {
                view = dList.getChildAt(i);
                cb = view.findViewById(R.id.checkBox);
                cb.setChecked(false);
                cb.setVisibility(View.GONE);
            }
            toggleActionBarElements();
        }

        else
        {
            finish();
        }
    }
}