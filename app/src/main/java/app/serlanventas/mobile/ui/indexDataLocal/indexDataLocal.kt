package app.serlanventas.mobile.ui.indexDataLocal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import app.serlanventas.mobile.R
import app.serlanventas.mobile.databinding.FragmentIndexLocaldataBinding

class indexDataLocal : Fragment() {
    private var _binding: FragmentIndexLocaldataBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIndexLocaldataBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cardClientes.setOnClickListener {
            findNavController().navigate(R.id.nav_initClientes)
        }

        binding.cardImpresora.setOnClickListener {
            findNavController().navigate(R.id.nav_impresoraConfig)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
