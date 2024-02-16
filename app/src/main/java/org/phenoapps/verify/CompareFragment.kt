package org.phenoapps.verify;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.app.Activity;
import android.content.Context
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView

import org.phenoapps.verify.databinding.FragmentCompareBinding;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */

public class CompareFragment : Fragment() {

    enum class Mode {
        Contains,
        Matches
    }

    private lateinit var view: View;

    private lateinit var barcodeScannerView: DecoratedBarcodeView
    private lateinit var firstEditText: EditText
    private lateinit var secondEditText: EditText
    private lateinit var imageView: ImageView

    private var mMode: Mode = Mode.Matches

    private var mFocused: Int = R.id.editText

    private val callback = object : BarcodeCallback {

        override fun barcodeResult(result: BarcodeResult) {

            barcodeScannerView.pause()

            result.text?.let {

                view.findViewById<EditText>(mFocused).setText(result.text ?: "")

                mFocused = when (mFocused) {
                    R.id.editText -> R.id.editText2
                    else -> R.id.editText
                }

                view.findViewById<EditText>(mFocused).requestFocus()
            }
            barcodeScannerView.resume()
        }
        override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var layoutView = inflater.inflate(R.layout.activity_compare, container, false);
        return layoutView;
    }


    override fun onStart() {
        super.onStart()
//        val choiceView = layoutInflater.inflate(R.layout.choice_compare_layout, null)
//
//        val radioGroup = choiceView.findViewById<RadioGroup>(R.id.compare_radio_group)

//        val containsRadioButton = radioGroup.findViewById<RadioButton>(R.id.radioButton)
//        val matchesRadioButton = radioGroup.findViewById<RadioButton>(R.id.radioButton2)

//        containsRadioButton.isChecked = true

//        val builder = AlertDialog.Builder(context as Context).apply {
//
//            setView(choiceView)
//
//            setTitle("Choose compare mode:")
//
//            setPositiveButton("OK") { _, _ ->
//                when (radioGroup.checkedRadioButtonId) {
//                    containsRadioButton.id -> mMode = CompareFragment.Mode.Contains
//                    matchesRadioButton.id -> mMode = CompareFragment.Mode.Matches
//                }
//            }
//        }
//
//        builder.show()

        imageView = view.findViewById(R.id.imageView)
        firstEditText = view.findViewById<EditText>(R.id.editText)
        secondEditText = view.findViewById<EditText>(R.id.editText2)
        barcodeScannerView = view.findViewById(R.id.zxing_barcode_scanner)


        firstEditText.setOnClickListener {
            mFocused = R.id.editText
        }

        secondEditText.setOnClickListener {
            mFocused = R.id.editText2
        }

        val watcher: TextWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {

                if (firstEditText.text.isNotEmpty() && secondEditText.text.isNotEmpty()) {

                    val first = firstEditText.text
                    val second = secondEditText.text
                    when (mMode) {
                        CompareFragment.Mode.Contains -> {
                            when {
                                first.contains(second) || second.contains(first) -> {
                                    imageView.setImageResource(R.drawable.ic_checkbox_marked_circle)
                                }
                                else -> imageView.setImageResource(R.drawable.ic_alpha_x_circle)
                            }
                        }
                        CompareFragment.Mode.Matches -> {
                            when {
                                firstEditText.text.toString() == secondEditText.text.toString() -> {
                                    imageView.setImageResource(R.drawable.ic_checkbox_marked_circle)
                                } else -> imageView.setImageResource(R.drawable.ic_alpha_x_circle)
                            }
                        }
                    }
                    imageView.visibility = View.VISIBLE
                }
            }

        }

        firstEditText.addTextChangedListener(watcher)
        secondEditText.addTextChangedListener(watcher)

        barcodeScannerView = view.findViewById(R.id.zxing_barcode_scanner)
        barcodeScannerView.barcodeView.cameraSettings.isContinuousFocusEnabled = true
        barcodeScannerView.barcodeView.cameraSettings.isBarcodeSceneModeEnabled = true
        barcodeScannerView.decodeContinuous(callback)

        val actionBar = (activity as AppCompatActivity).supportActionBar;
        if (actionBar != null) {
            actionBar.title = "Compare Barcodes"
            actionBar.themedContext
        }

        imageView.setOnClickListener {
            firstEditText.setText("")
            secondEditText.setText("")
            firstEditText.requestFocus()
            imageView.visibility = View.INVISIBLE
        }
    }

    override fun onResume() {
        super.onResume()

        barcodeScannerView.resume()
    }

    override fun onPause() {
        super.onPause()

        barcodeScannerView.pause()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        this.view = view;
    }
}