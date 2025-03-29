// Generated by view binder compiler. Do not edit!
package app.serlanventas.mobile.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import app.serlanventas.mobile.R;
import com.google.android.material.button.MaterialButton;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;

public final class FragmentModuleGalponesBinding implements ViewBinding {
  @NonNull
  private final ConstraintLayout rootView;

  @NonNull
  public final MaterialButton btnAddGalpon;

  @NonNull
  public final MaterialButton btnSincronizarGalpones;

  @NonNull
  public final RecyclerView recyclerViewGalpon;

  @NonNull
  public final TextView seccionDatosGalpones;

  private FragmentModuleGalponesBinding(@NonNull ConstraintLayout rootView,
      @NonNull MaterialButton btnAddGalpon, @NonNull MaterialButton btnSincronizarGalpones,
      @NonNull RecyclerView recyclerViewGalpon, @NonNull TextView seccionDatosGalpones) {
    this.rootView = rootView;
    this.btnAddGalpon = btnAddGalpon;
    this.btnSincronizarGalpones = btnSincronizarGalpones;
    this.recyclerViewGalpon = recyclerViewGalpon;
    this.seccionDatosGalpones = seccionDatosGalpones;
  }

  @Override
  @NonNull
  public ConstraintLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static FragmentModuleGalponesBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static FragmentModuleGalponesBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.fragment_module_galpones, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static FragmentModuleGalponesBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.btn_add_galpon;
      MaterialButton btnAddGalpon = ViewBindings.findChildViewById(rootView, id);
      if (btnAddGalpon == null) {
        break missingId;
      }

      id = R.id.btn_sincronizar_galpones;
      MaterialButton btnSincronizarGalpones = ViewBindings.findChildViewById(rootView, id);
      if (btnSincronizarGalpones == null) {
        break missingId;
      }

      id = R.id.recyclerViewGalpon;
      RecyclerView recyclerViewGalpon = ViewBindings.findChildViewById(rootView, id);
      if (recyclerViewGalpon == null) {
        break missingId;
      }

      id = R.id.seccion_datos_galpones;
      TextView seccionDatosGalpones = ViewBindings.findChildViewById(rootView, id);
      if (seccionDatosGalpones == null) {
        break missingId;
      }

      return new FragmentModuleGalponesBinding((ConstraintLayout) rootView, btnAddGalpon,
          btnSincronizarGalpones, recyclerViewGalpon, seccionDatosGalpones);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}
