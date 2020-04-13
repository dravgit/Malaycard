package com.example.malaycard

import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import khttp.post
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File


class MainActivity : AppCompatActivity() {
    private val PERMISSION_CODE = 1000;
    private val IMAGE_CAPTURE_CODE = 1001

    var image_uri: Uri? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //button click
        btn_camera.setOnClickListener {
            val intent = Intent(this@MainActivity,CameraPreview::class.java)
            startActivityForResult(intent,100)
//            //if system os is Marshmallow or Above, we need to request runtime permission
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
//                if (checkSelfPermission(android.Manifest.permission.CAMERA)
//                    == PackageManager.PERMISSION_DENIED ||
//                    checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
//                    == PackageManager.PERMISSION_DENIED){
//                    //permission was not enabled
//                    val permission = arrayOf(android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
//                    //show popup to request permission
//                    requestPermissions(permission, PERMISSION_CODE)
//                }
//                else{
//                    //permission already granted
//                    openCamera()
//                }
//            }
//            else{
//                //system os is < marshmallow
//                openCamera()
//            }
        }
    }



    private fun openCamera() {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "New Picture")
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera")
        image_uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        //camera intent
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra("android.intent.extra.quickCapture",true);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri)
        startActivityForResult(cameraIntent, IMAGE_CAPTURE_CODE)
    }

    private fun encodeImage(bm: Bitmap): String? {
        val baos = ByteArrayOutputStream()
        bm.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val b = baos.toByteArray()
        return Base64.encodeToString(b, Base64.DEFAULT)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        //called when user presses ALLOW or DENY from Permission Request Popup
        when(requestCode){
            PERMISSION_CODE -> {
                if (grantResults.size > 0 && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED){
                    //permission from popup was granted
                    openCamera()
                }
                else{
                    //permission from popup was denied
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //called when image was captured from camera intent
        if (resultCode == 100){
            val path = data!!.getStringExtra("result")
            Log.e("file",path)
            val file = File("$path")
            val myBitmap = BitmapFactory.decodeFile(file.absolutePath)
            val show = Bitmap.createBitmap(myBitmap,0,200, 1900,1300)
            val testbit = Bitmap.createBitmap(myBitmap,50,400,800,1000)
            //set image captured to image view
            runOnUiThread {
                iVcapture.setImageBitmap(show)
            }
//            val drawable = iVcapture.getDrawable() as BitmapDrawable
//            val bitmap = drawable.bitmap
//            val ocrbitmap = Bitmap.createBitmap(bitmap, 0, 1050, 800, 1000)
//            iVcapture.setImageBitmap(ocrbitmap)
            val encode = encodeImage(testbit)
//            Log.e("getbitmap",encode)
            val dialog = ProgressDialog.show(this@MainActivity, "",
                    "Loading. Please wait...", true)
            Thread(Runnable {
                try {
                    val r = post("https://simapi.kachendigital.co.th/oauth/token",data = mapOf("grant_type" to "client_credentials",
                        "client_id" to "4",
                        "client_secret" to "xZPyMFwN5ghKxMp3H20nia1BvEnwxC5JqoMupdVO"))
                    val obj : JSONObject = r.jsonObject
                    try{
                        val token : String? = obj["token_type"].toString()
                        val access : String? = obj["access_token"].toString()
                        val response = khttp.get(
                            url = "https://simapi.kachendigital.co.th/api/v1/ocr",
                            headers = mapOf("Authorization" to "$token $access"),
                            data = mapOf("image" to encode))
                        val obj : JSONObject = response.jsonObject
                        try{
                            val resultArray = obj.getJSONArray("data")
                            val result = resultArray.getJSONObject(0)
                            val resultObjec = result.getString("description")
                            val idnumber = "ID : " + resultObjec.split("\n")[0]
                            val name = "Name : " + resultObjec.split("\n")[1]
                            val address = "Address : " + resultObjec.split("\n")[2] + resultObjec.split("\n")[3] +
                                                 resultObjec.split("\n")[4] +resultObjec.split("\n")[5] +
                                                 resultObjec.split("\n")[6]
                            runOnUiThread {
                                // Stuff that updates the UI
                                tV1.setText(idnumber)
                                tV2.setText(name)
                                tV3.setText(address)
                                dialog.dismiss()
                            }
                        }catch (ex: Exception){
                            Log.d("Exception", ex.toString())
                        }
                    } catch (ex: Exception) {
                        Log.d("Exception", ex.toString())
                    }
                } catch (ex: Exception) {
                    Log.d("Exception", ex.toString())
                }
            }).start()
        }
    }
}


