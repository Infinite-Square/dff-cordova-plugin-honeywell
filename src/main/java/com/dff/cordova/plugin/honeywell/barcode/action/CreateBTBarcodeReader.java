package com.dff.cordova.plugin.honeywell.barcode.action;

import com.dff.cordova.plugin.common.log.CordovaPluginLog;
import com.dff.cordova.plugin.honeywell.barcode.BarcodeListener;
import com.dff.cordova.plugin.honeywell.common.BarcodeReaderManager;
import com.honeywell.aidc.AidcManager;
import com.honeywell.aidc.ScannerUnavailableException;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.json.JSONArray;
import org.json.JSONObject;

import com.dff.cordova.plugin.honeywell.common.GsonNamingStrategy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.json.*;
import java.util.List;

import com.honeywell.aidc.BarcodeReaderInfo;

public class CreateBTBarcodeReader extends HoneywellAction {

    private static final String TAG = "com.dff.cordova.plugin.honeywell.barcode.action.CreateBTBarcodeReader";
    public static final String ACTION_NAME = "createBTBarcodeReader";

    public CreateBTBarcodeReader(String action, JSONArray args, CallbackContext callbackContext,
                               CordovaInterface cordova, BarcodeReaderManager barcodeReaderManager, AidcManager aidcManager,
                               BarcodeListener barcodeListener) {
        super(action, args, callbackContext, cordova, barcodeReaderManager,  aidcManager, barcodeListener);
    }

    public static final String JSON_ARGS_NAME = "name";
    public static final String[] JSON_ARGS = { JSON_ARGS_NAME };

    @Override
    public void run() {
        try {
            // check aidc manager (really necessary?)
            if(this.aidcManager != null) {

                // check for already connected barcode reader
                if(this.barcodeReaderManager.getInstance() == null) {

                    // get optional name parameter
                    String[] emptyArray = {};
                    JSONObject jsonArgs = super.checkJsonArgs(args, emptyArray);
                    // String name = jsonArgs.optString(JSON_ARGS_NAME, null);

                    try
                    {

						List<BarcodeReaderInfo> listOfConnectedBarcodeReaders = this.aidcManager.listConnectedBarcodeDevices();

						if (listOfConnectedBarcodeReaders.size() == 0) {
							// no connected devices
							this.callbackContext.success(NO_CONNECTED_DEVICES);
						} else {
							// Gson conversion code
							Gson gson = new GsonBuilder().setFieldNamingStrategy(new GsonNamingStrategy()).create();
							String json = gson.toJson(listOfConnectedBarcodeReaders);
							JSONArray jsonArray = new JSONArray(json);

							boolean isOneConnectedDeviceIsBT = false;

							for (int i = 0; i < jsonArray.length(); i++) {

								// if firendlyName isn't "internal" => get his name
								String friendlyName = jsonArray.getJSONObject(i).getString("friendlyName");
								if(!friendlyName.toLowerCase().contains("internal")) {
									String BTName = jsonArray.getJSONObject(i).getString("name");
									this.barcodeReaderManager.setInstance(this.aidcManager.createBarcodeReader(BTName));
									isOneConnectedDeviceIsBT =  true;
								}
							}
							if (isOneConnectedDeviceIsBT) {
								this.barcodeReaderManager.getInstance().claim();
								this.barcodeReaderManager.getInstance().addBarcodeListener(this.barcodeListener);
		
								// return barcode reader info
								this.callbackContext.success(BarcodeReaderGetInfo.getBarCodeReaderInfoAsJSOnObject(this.barcodeReaderManager));
							} else {
								this.callbackContext.success(NO_CONNECTED_DEVICES);
							}
							
						}
                    }
                    catch (ScannerUnavailableException e) {
                        this.callbackContext.error(e.getMessage());
                        CordovaPluginLog.e(TAG, e.getMessage(), e);
                    }
                    catch (Exception e)
                    {
                        this.callbackContext.error(e.getMessage());
                        CordovaPluginLog.e(TAG, e.getMessage(), e);
                    }
                }
                else
                {
                    // a reader is already connected
                    this.callbackContext.error(BARCODE_READER_ALREADY_ADDED);
                }
            }
            else
            {
                // aidc manager is initialized from pluginInitialize method.
                // this error below should never occur.
                callbackContext.error(AICD_NOT_INIT);
            }
        }
        catch (Exception e) {
            CordovaPluginLog.e(TAG, e.getMessage(), e);
            this.callbackContext.error(e.getMessage());
        }
    }
}
