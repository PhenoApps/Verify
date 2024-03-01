package org.phenoapps.verify

import android.app.Activity
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView

class CompareActivity : AppCompatActivity() {

    enum class Mode {
        Contains,
        Matches
    }

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
                findViewById<EditText>(mFocused).setText(result.text ?: "")
                mFocused = if (mFocused == R.id.editText) R.id.editText2 else R.id.editText
                findViewById<EditText>(mFocused).requestFocus()
            }

            barcodeScannerView.resume()
        }

        override fun possibleResultPoints(resultPoints: List<ResultPoint>) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_compare)

        initializeViews()
        setupBarcodeScanner()
        setupRadioGroup()
        setupEditTextListeners()
        setupImageViewListener()
        configureActionBar()
    }

    private fun initializeViews() {
        imageView = findViewById(R.id.imageView)
        firstEditText = findViewById(R.id.editText)
        secondEditText = findViewById(R.id.editText2)
        barcodeScannerView = findViewById(R.id.zxing_barcode_scanner)
    }

    private fun setupBarcodeScanner() {
        barcodeScannerView.barcodeView.cameraSettings.isContinuousFocusEnabled = true
        barcodeScannerView.barcodeView.cameraSettings.isBarcodeSceneModeEnabled = true
        barcodeScannerView.decodeContinuous(callback)
    }

    private fun setupRadioGroup() {
        val radioGroup = findViewById<RadioGroup>(R.id.compare_radio_group)
        val containsRadioButton = findViewById<RadioButton>(R.id.radioButton_contains)
        val matchesRadioButton = findViewById<RadioButton>(R.id.radioButton_matches)

        containsRadioButton.isChecked = true

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            mMode = when (checkedId) {
                R.id.radioButton_contains -> Mode.Contains
                R.id.radioButton_matches -> Mode.Matches
                else -> mMode
            }
        }
    }

    private fun setupEditTextListeners() {
        val watcher: TextWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateImageView()
            }
        }

        firstEditText.addTextChangedListener(watcher)
        secondEditText.addTextChangedListener(watcher)
    }

    private fun updateImageView() {
        if (firstEditText.text.isNotEmpty() && secondEditText.text.isNotEmpty()) {
            val first = firstEditText.text.toString()
            val second = secondEditText.text.toString()

            when (mMode) {
                Mode.Contains -> {
                    val match = first.contains(second) || second.contains(first)
                    imageView.setImageResource(if (match) R.drawable.ic_checkbox_marked_circle else R.drawable.ic_alpha_x_circle)
                }
                Mode.Matches -> {
                    val match = first == second
                    imageView.setImageResource(if (match) R.drawable.ic_checkbox_marked_circle else R.drawable.ic_alpha_x_circle)
                }
            }
            imageView.visibility = View.VISIBLE
        }
    }

    private fun setupImageViewListener() {
        imageView.setOnClickListener {
            firstEditText.text.clear()
            secondEditText.text.clear()
            imageView.visibility = View.INVISIBLE
        }
    }

    private fun configureActionBar() {
        supportActionBar?.apply {
            title = "Compare Barcodes"
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                setResult(Activity.RESULT_OK)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
