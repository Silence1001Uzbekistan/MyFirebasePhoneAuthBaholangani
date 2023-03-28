package com.example.firebasephoneauthtuesday

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.firebasephoneauthtuesday.databinding.FragmentInputCodeBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import java.util.concurrent.TimeUnit


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER

/**
 * A simple [Fragment] subclass.
 * Use the [SignUpFragment.newInstance] factory method to
 * create an instance of this fragment.
 */

private const val ARG_PARAM1 = "phone"
private const val ARG_PARAM2 = "phone2"

class InputCodeFragment : Fragment() {

    private var phoneNumber: String? = null
    private var phoneNumber2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            phoneNumber = it.getString(ARG_PARAM1)
            phoneNumber2 = it.getString(ARG_PARAM2)
        }
    }

    lateinit var binding: FragmentInputCodeBinding
    lateinit var auth: FirebaseAuth
    lateinit var storedVerificationId: String
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var timer: CountDownTimer? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentInputCodeBinding.inflate(layoutInflater)

        auth = FirebaseAuth.getInstance()
        auth.setLanguageCode("uz")
        loadTimer()
        loadDataToView()


//      To apply the default app language instead of explicitly setting it.
//      auth.useAppLanguage()

        Log.d("AAAA", "phone number: $phoneNumber")
        phoneNumber?.let { sentVerificationCode(it) }


        binding.inputEt.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                verifyCode()
                val view = activity?.currentFocus
                if (view != null) {
                    val imm: InputMethodManager =
                        activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                }
            }
            true
        }

        binding.inputEt.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                if (binding.inputEt.text!!.length == 10)     //size as per your requirement
                {

                    val bundle = Bundle()
                    bundle.putString("number2", phoneNumber2)
                    findNavController().navigate(com.example.firebasephoneauthtuesday.R.id.succesFragment,bundle)
                    //Toast.makeText(requireContext(), "AAAA", Toast.LENGTH_SHORT).show()
                }

            }

            override fun beforeTextChanged(
                s: CharSequence?, start: Int,
                count: Int, after: Int
            ) {


            }

            override fun afterTextChanged(s: Editable?) {


            }
        })

/*        binding.inputEt.addTextChangedListener {
            if (it.toString().length == 6) {
                verifyCode()

            }
        }*/

        binding.resend.setOnClickListener {
            phoneNumber?.let { it1 -> resendCode(it1) }
            timer?.cancel()
            loadTimer()
            binding.inputEt.isEnabled = true
        }

        return binding.root

    }

    private fun loadTimer() {


        timer = object : CountDownTimer(60000, 1000) {
            @SuppressLint("SetTextI18n")
            override fun onTick(millisUntilFinished: Long) {
                if (millisUntilFinished / 1000 < 10) {
                    binding.timer.text = "00:0" + millisUntilFinished / 1000
                } else {
                    binding.timer.text = "00:" + millisUntilFinished / 1000
                }
            }

            override fun onFinish() {
                binding.timer.text = "00:00"
                binding.inputEt.isEnabled = false

            }
        }
        timer?.start()
    }

    override fun onStop() {
        super.onStop()
        timer?.cancel()
    }

    private fun loadDataToView() {
        binding.text1.text =
            "Bir martalik kod  ${phoneNumber2?.replaceAfter("-", "**-**")} raqamiga yuborildi"
    }

    private fun verifyCode() {
        val code = binding.inputEt.text.toString().replace(" ", "", true)
        if (code.length == 6) {
            val credential = PhoneAuthProvider.getCredential(storedVerificationId, code)
            signInWithPhoneAuthCredential(credential)

        }
    }

    fun sentVerificationCode(phoneNumber: String) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)       // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(requireActivity())                 // Activity (for callback binding)
            .setCallbacks(callbacks)          // OnVerificationStateChangedCallbacks
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun resendCode(phoneNumber: String) {
        if (resendToken != null) {
            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneNumber)       // Phone number to verify
                .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                .setActivity(requireActivity())                 // Activity (for callback binding)
                .setCallbacks(callbacks)          // OnVerificationStateChangedCallbacks
                .setForceResendingToken(resendToken!!)
                .build()
            PhoneAuthProvider.verifyPhoneNumber(options)
        }
    }


    private var callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            // This callback will be invoked in two situations:
            // 1 - Instant verification. In some cases the phone number can be instantly
            //     verified without needing to send or enter a verification code.
            // 2 - Auto-retrieval. On some devices Google Play services can automatically
            //     detect the incoming verification SMS and perform verification without
            //     user action.
            Log.d("AAAA", "onVerificationCompleted:$credential")
            signInWithPhoneAuthCredential(credential)
        }

        override fun onVerificationFailed(e: FirebaseException) {
            // This callback is invoked in an invalid request for verification is made,
            // for instance if the the phone number format is not valid.
            Log.w("AAAA", "onVerificationFailed", e)

            if (e is FirebaseAuthInvalidCredentialsException) {
                // Invalid request
            } else if (e is FirebaseTooManyRequestsException) {
                // The SMS quota for the project has been exceeded
            }

            // Show a message and update the UI
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            // The SMS verification code has been sent to the provided phone number, we
            // now need to ask the user to enter the code and then construct a credential
            // by combining the code with a verification ID.
            Log.d("AAAA", "onCodeSent:$verificationId")

            // Save verification ID and resending token so we can use them later
            storedVerificationId = verificationId
            resendToken = token
        }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Sign in success, update UI with the signed-in user's information
                Log.d("AAAA", "signInWithCredential:success")

                val user = task.result?.user?.phoneNumber
                Log.d("AAAA", "signInWithPhoneAuthCredential: $user")

                val bundle = Bundle()
                bundle.putString("number2", phoneNumber2)
                findNavController().navigate(com.example.firebasephoneauthtuesday.R.id.succesFragment)

            } else {
                // Sign in failed, display a message and update the UI
                Log.w("AAAA", "signInWithCredential:failure", task.exception)

                if (task.exception is FirebaseAuthInvalidCredentialsException) {
                    // The verification code entered was invalid
                    Snackbar.make(binding.root, "Kiritilgan kod noto'g'ri!", Snackbar.LENGTH_LONG)
                        .show()
                }
                // Update UI
            }
        }
    }


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment InputCodeFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            InputCodeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}