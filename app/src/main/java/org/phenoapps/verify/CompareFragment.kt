package org.phenoapps.verify

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import org.phenoapps.verify.ViewModel.CompareViewModel

class CompareFragment : Fragment() {

    private lateinit var view: View
    private lateinit var barcodeScannerView: DecoratedBarcodeView
    private lateinit var firstEditText: EditText
    private lateinit var secondEditText: EditText
    private lateinit var imageView: ImageView
    private lateinit var viewModel: CompareViewModel

    private var mFocused: Int = R.id.editText

    private val callback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult) {
            barcodeScannerView.pause()

            result.text?.let {
                view.findViewById<EditText>(mFocused).setText(it)
                mFocused = if (mFocused == R.id.editText) R.id.editText2 else R.id.editText
                view.findViewById<EditText>(mFocused).requestFocus()
            }

            barcodeScannerView.resume()
        }

        override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[CompareViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.activity_compare, container, false).also { view = it }
    }

    override fun onStart() {
        super.onStart()

        imageView = view.findViewById(R.id.imageView)
        firstEditText = view.findViewById(R.id.editText)
        secondEditText = view.findViewById(R.id.editText2)
        barcodeScannerView = view.findViewById(R.id.zxing_barcode_scanner)

        firstEditText.setOnClickListener { mFocused = R.id.editText }
        secondEditText.setOnClickListener { mFocused = R.id.editText2 }

        val watcher: TextWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateImageView()
            }
        }

        firstEditText.addTextChangedListener(watcher)
        secondEditText.addTextChangedListener(watcher)

        setupBarcodeScanner()
        setupRadioGroup()
        setupImageViewListener()

        (activity as AppCompatActivity).supportActionBar?.title = "Compare Barcodes"
    }

    private fun setupBarcodeScanner() {
        barcodeScannerView.barcodeView.cameraSettings.isContinuousFocusEnabled = true
        barcodeScannerView.barcodeView.cameraSettings.isBarcodeSceneModeEnabled = true
        barcodeScannerView.decodeContinuous(callback)
    }

    private fun setupRadioGroup() {
        val radioGroup = view.findViewById<RadioGroup>(R.id.compare_radio_group)
        radioGroup.check(if (viewModel.getMode() == CompareViewModel.Mode.Contains) R.id.radioButton_contains else R.id.radioButton_matches)

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            viewModel.setMode(if (checkedId == R.id.radioButton_contains) CompareViewModel.Mode.Contains else CompareViewModel.Mode.Matches)
        }
    }

    private fun setupImageViewListener() {
        imageView.setOnClickListener {
            firstEditText.text.clear()
            secondEditText.text.clear()
            imageView.visibility = View.INVISIBLE
        }
    }

    private fun updateImageView() {
        if (firstEditText.text.isNotEmpty() && secondEditText.text.isNotEmpty()) {
            val first = firstEditText.text.toString()
            val second = secondEditText.text.toString()
            val match = when (viewModel.getMode()) {
                CompareViewModel.Mode.Contains -> first.contains(second) || second.contains(first)
                CompareViewModel.Mode.Matches -> first == second
            }
            imageView.setImageResource(if (match) R.drawable.ic_checkbox_marked_circle else R.drawable.ic_alpha_x_circle)
            imageView.visibility = View.VISIBLE
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
}
