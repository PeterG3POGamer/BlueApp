// Generated by view binder compiler. Do not edit!
package app.serlanventas.mobile.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import app.serlanventas.mobile.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;

public final class FragmentPesosBinding implements ViewBinding {
  @NonNull
  private final FrameLayout rootView;

  @NonNull
  public final EditText PrecioKilo;

  @NonNull
  public final LinearLayout accordionContent;

  @NonNull
  public final LinearLayout accordionHeader;

  @NonNull
  public final ImageView arrow;

  @NonNull
  public final MaterialButton botonCliente;

  @NonNull
  public final ImageButton botonDeletePeso;

  @NonNull
  public final ImageButton botonEnviar;

  @NonNull
  public final Button botonGuardar;

  @NonNull
  public final ImageButton botonGuardarPeso;

  @NonNull
  public final ImageButton botonLimpiar;

  @NonNull
  public final MaterialButton botonLimpiarCliente;

  @NonNull
  public final MaterialButton btnSincronizarPesos;

  @NonNull
  public final SwitchMaterial checkboxConPollos;

  @NonNull
  public final TextView conPollos;

  @NonNull
  public final TextView contadorJabas;

  @NonNull
  public final TextView deviceConnected;

  @NonNull
  public final LinearLayout deviceConnectedLayout;

  @NonNull
  public final FrameLayout fragmentContainerBluetooth;

  @NonNull
  public final LinearLayout fragmentPesos;

  @NonNull
  public final LinearLayout headerLayout;

  @NonNull
  public final TextView idJabas;

  @NonNull
  public final TextView idListPeso;

  @NonNull
  public final ImageView imagenPaquete;

  @NonNull
  public final TextInputEditText inputCantPollos;

  @NonNull
  public final TextInputLayout inputLayoutPollos;

  @NonNull
  public final TextInputEditText inputNumeroJabas;

  @NonNull
  public final TextInputEditText inputPesoKg;

  @NonNull
  public final ImageView loadingGif;

  @NonNull
  public final TextView numeroJabas;

  @NonNull
  public final TextView numeroPollos;

  @NonNull
  public final View overlay;

  @NonNull
  public final TextView pesoKg;

  @NonNull
  public final RecyclerView recyclerViewJabas;

  @NonNull
  public final TextView seccionNucleo;

  @NonNull
  public final Spinner selectEstablecimiento;

  @NonNull
  public final Spinner selectGalpon;

  @NonNull
  public final Spinner selectListpesos;

  @NonNull
  public final TextInputEditText textDocCli;

  @NonNull
  public final TextInputEditText textNomCli;

  @NonNull
  public final TextView totalPagarPreview;

  private FragmentPesosBinding(@NonNull FrameLayout rootView, @NonNull EditText PrecioKilo,
      @NonNull LinearLayout accordionContent, @NonNull LinearLayout accordionHeader,
      @NonNull ImageView arrow, @NonNull MaterialButton botonCliente,
      @NonNull ImageButton botonDeletePeso, @NonNull ImageButton botonEnviar,
      @NonNull Button botonGuardar, @NonNull ImageButton botonGuardarPeso,
      @NonNull ImageButton botonLimpiar, @NonNull MaterialButton botonLimpiarCliente,
      @NonNull MaterialButton btnSincronizarPesos, @NonNull SwitchMaterial checkboxConPollos,
      @NonNull TextView conPollos, @NonNull TextView contadorJabas,
      @NonNull TextView deviceConnected, @NonNull LinearLayout deviceConnectedLayout,
      @NonNull FrameLayout fragmentContainerBluetooth, @NonNull LinearLayout fragmentPesos,
      @NonNull LinearLayout headerLayout, @NonNull TextView idJabas, @NonNull TextView idListPeso,
      @NonNull ImageView imagenPaquete, @NonNull TextInputEditText inputCantPollos,
      @NonNull TextInputLayout inputLayoutPollos, @NonNull TextInputEditText inputNumeroJabas,
      @NonNull TextInputEditText inputPesoKg, @NonNull ImageView loadingGif,
      @NonNull TextView numeroJabas, @NonNull TextView numeroPollos, @NonNull View overlay,
      @NonNull TextView pesoKg, @NonNull RecyclerView recyclerViewJabas,
      @NonNull TextView seccionNucleo, @NonNull Spinner selectEstablecimiento,
      @NonNull Spinner selectGalpon, @NonNull Spinner selectListpesos,
      @NonNull TextInputEditText textDocCli, @NonNull TextInputEditText textNomCli,
      @NonNull TextView totalPagarPreview) {
    this.rootView = rootView;
    this.PrecioKilo = PrecioKilo;
    this.accordionContent = accordionContent;
    this.accordionHeader = accordionHeader;
    this.arrow = arrow;
    this.botonCliente = botonCliente;
    this.botonDeletePeso = botonDeletePeso;
    this.botonEnviar = botonEnviar;
    this.botonGuardar = botonGuardar;
    this.botonGuardarPeso = botonGuardarPeso;
    this.botonLimpiar = botonLimpiar;
    this.botonLimpiarCliente = botonLimpiarCliente;
    this.btnSincronizarPesos = btnSincronizarPesos;
    this.checkboxConPollos = checkboxConPollos;
    this.conPollos = conPollos;
    this.contadorJabas = contadorJabas;
    this.deviceConnected = deviceConnected;
    this.deviceConnectedLayout = deviceConnectedLayout;
    this.fragmentContainerBluetooth = fragmentContainerBluetooth;
    this.fragmentPesos = fragmentPesos;
    this.headerLayout = headerLayout;
    this.idJabas = idJabas;
    this.idListPeso = idListPeso;
    this.imagenPaquete = imagenPaquete;
    this.inputCantPollos = inputCantPollos;
    this.inputLayoutPollos = inputLayoutPollos;
    this.inputNumeroJabas = inputNumeroJabas;
    this.inputPesoKg = inputPesoKg;
    this.loadingGif = loadingGif;
    this.numeroJabas = numeroJabas;
    this.numeroPollos = numeroPollos;
    this.overlay = overlay;
    this.pesoKg = pesoKg;
    this.recyclerViewJabas = recyclerViewJabas;
    this.seccionNucleo = seccionNucleo;
    this.selectEstablecimiento = selectEstablecimiento;
    this.selectGalpon = selectGalpon;
    this.selectListpesos = selectListpesos;
    this.textDocCli = textDocCli;
    this.textNomCli = textNomCli;
    this.totalPagarPreview = totalPagarPreview;
  }

  @Override
  @NonNull
  public FrameLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static FragmentPesosBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static FragmentPesosBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.fragment_pesos, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static FragmentPesosBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.PrecioKilo;
      EditText PrecioKilo = ViewBindings.findChildViewById(rootView, id);
      if (PrecioKilo == null) {
        break missingId;
      }

      id = R.id.accordion_content;
      LinearLayout accordionContent = ViewBindings.findChildViewById(rootView, id);
      if (accordionContent == null) {
        break missingId;
      }

      id = R.id.accordion_header;
      LinearLayout accordionHeader = ViewBindings.findChildViewById(rootView, id);
      if (accordionHeader == null) {
        break missingId;
      }

      id = R.id.arrow;
      ImageView arrow = ViewBindings.findChildViewById(rootView, id);
      if (arrow == null) {
        break missingId;
      }

      id = R.id.boton_Cliente;
      MaterialButton botonCliente = ViewBindings.findChildViewById(rootView, id);
      if (botonCliente == null) {
        break missingId;
      }

      id = R.id.boton_delete_peso;
      ImageButton botonDeletePeso = ViewBindings.findChildViewById(rootView, id);
      if (botonDeletePeso == null) {
        break missingId;
      }

      id = R.id.boton_Enviar;
      ImageButton botonEnviar = ViewBindings.findChildViewById(rootView, id);
      if (botonEnviar == null) {
        break missingId;
      }

      id = R.id.boton_guardar;
      Button botonGuardar = ViewBindings.findChildViewById(rootView, id);
      if (botonGuardar == null) {
        break missingId;
      }

      id = R.id.boton_guardar_peso;
      ImageButton botonGuardarPeso = ViewBindings.findChildViewById(rootView, id);
      if (botonGuardarPeso == null) {
        break missingId;
      }

      id = R.id.boton_Limpiar;
      ImageButton botonLimpiar = ViewBindings.findChildViewById(rootView, id);
      if (botonLimpiar == null) {
        break missingId;
      }

      id = R.id.botonLimpiarCliente;
      MaterialButton botonLimpiarCliente = ViewBindings.findChildViewById(rootView, id);
      if (botonLimpiarCliente == null) {
        break missingId;
      }

      id = R.id.btn_sincronizar_pesos;
      MaterialButton btnSincronizarPesos = ViewBindings.findChildViewById(rootView, id);
      if (btnSincronizarPesos == null) {
        break missingId;
      }

      id = R.id.checkboxConPollos;
      SwitchMaterial checkboxConPollos = ViewBindings.findChildViewById(rootView, id);
      if (checkboxConPollos == null) {
        break missingId;
      }

      id = R.id.con_pollos;
      TextView conPollos = ViewBindings.findChildViewById(rootView, id);
      if (conPollos == null) {
        break missingId;
      }

      id = R.id.contador_jabas;
      TextView contadorJabas = ViewBindings.findChildViewById(rootView, id);
      if (contadorJabas == null) {
        break missingId;
      }

      id = R.id.device_connected;
      TextView deviceConnected = ViewBindings.findChildViewById(rootView, id);
      if (deviceConnected == null) {
        break missingId;
      }

      id = R.id.device_connected_layout;
      LinearLayout deviceConnectedLayout = ViewBindings.findChildViewById(rootView, id);
      if (deviceConnectedLayout == null) {
        break missingId;
      }

      id = R.id.fragment_container_bluetooth;
      FrameLayout fragmentContainerBluetooth = ViewBindings.findChildViewById(rootView, id);
      if (fragmentContainerBluetooth == null) {
        break missingId;
      }

      id = R.id.fragment_pesos;
      LinearLayout fragmentPesos = ViewBindings.findChildViewById(rootView, id);
      if (fragmentPesos == null) {
        break missingId;
      }

      id = R.id.header_layout;
      LinearLayout headerLayout = ViewBindings.findChildViewById(rootView, id);
      if (headerLayout == null) {
        break missingId;
      }

      id = R.id.id_jabas;
      TextView idJabas = ViewBindings.findChildViewById(rootView, id);
      if (idJabas == null) {
        break missingId;
      }

      id = R.id.idListPeso;
      TextView idListPeso = ViewBindings.findChildViewById(rootView, id);
      if (idListPeso == null) {
        break missingId;
      }

      id = R.id.imagen_paquete;
      ImageView imagenPaquete = ViewBindings.findChildViewById(rootView, id);
      if (imagenPaquete == null) {
        break missingId;
      }

      id = R.id.input_cant_pollos;
      TextInputEditText inputCantPollos = ViewBindings.findChildViewById(rootView, id);
      if (inputCantPollos == null) {
        break missingId;
      }

      id = R.id.input_layout_pollos;
      TextInputLayout inputLayoutPollos = ViewBindings.findChildViewById(rootView, id);
      if (inputLayoutPollos == null) {
        break missingId;
      }

      id = R.id.input_numero_jabas;
      TextInputEditText inputNumeroJabas = ViewBindings.findChildViewById(rootView, id);
      if (inputNumeroJabas == null) {
        break missingId;
      }

      id = R.id.input_peso_kg;
      TextInputEditText inputPesoKg = ViewBindings.findChildViewById(rootView, id);
      if (inputPesoKg == null) {
        break missingId;
      }

      id = R.id.loadingGif;
      ImageView loadingGif = ViewBindings.findChildViewById(rootView, id);
      if (loadingGif == null) {
        break missingId;
      }

      id = R.id.numero_jabas;
      TextView numeroJabas = ViewBindings.findChildViewById(rootView, id);
      if (numeroJabas == null) {
        break missingId;
      }

      id = R.id.numero_pollos;
      TextView numeroPollos = ViewBindings.findChildViewById(rootView, id);
      if (numeroPollos == null) {
        break missingId;
      }

      id = R.id.overlay;
      View overlay = ViewBindings.findChildViewById(rootView, id);
      if (overlay == null) {
        break missingId;
      }

      id = R.id.peso_kg;
      TextView pesoKg = ViewBindings.findChildViewById(rootView, id);
      if (pesoKg == null) {
        break missingId;
      }

      id = R.id.recyclerViewJabas;
      RecyclerView recyclerViewJabas = ViewBindings.findChildViewById(rootView, id);
      if (recyclerViewJabas == null) {
        break missingId;
      }

      id = R.id.seccion_nucleo;
      TextView seccionNucleo = ViewBindings.findChildViewById(rootView, id);
      if (seccionNucleo == null) {
        break missingId;
      }

      id = R.id.select_establecimiento;
      Spinner selectEstablecimiento = ViewBindings.findChildViewById(rootView, id);
      if (selectEstablecimiento == null) {
        break missingId;
      }

      id = R.id.select_galpon;
      Spinner selectGalpon = ViewBindings.findChildViewById(rootView, id);
      if (selectGalpon == null) {
        break missingId;
      }

      id = R.id.select_listpesos;
      Spinner selectListpesos = ViewBindings.findChildViewById(rootView, id);
      if (selectListpesos == null) {
        break missingId;
      }

      id = R.id.textDocCli;
      TextInputEditText textDocCli = ViewBindings.findChildViewById(rootView, id);
      if (textDocCli == null) {
        break missingId;
      }

      id = R.id.textNomCli;
      TextInputEditText textNomCli = ViewBindings.findChildViewById(rootView, id);
      if (textNomCli == null) {
        break missingId;
      }

      id = R.id.totalPagarPreview;
      TextView totalPagarPreview = ViewBindings.findChildViewById(rootView, id);
      if (totalPagarPreview == null) {
        break missingId;
      }

      return new FragmentPesosBinding((FrameLayout) rootView, PrecioKilo, accordionContent,
          accordionHeader, arrow, botonCliente, botonDeletePeso, botonEnviar, botonGuardar,
          botonGuardarPeso, botonLimpiar, botonLimpiarCliente, btnSincronizarPesos,
          checkboxConPollos, conPollos, contadorJabas, deviceConnected, deviceConnectedLayout,
          fragmentContainerBluetooth, fragmentPesos, headerLayout, idJabas, idListPeso,
          imagenPaquete, inputCantPollos, inputLayoutPollos, inputNumeroJabas, inputPesoKg,
          loadingGif, numeroJabas, numeroPollos, overlay, pesoKg, recyclerViewJabas, seccionNucleo,
          selectEstablecimiento, selectGalpon, selectListpesos, textDocCli, textNomCli,
          totalPagarPreview);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}
