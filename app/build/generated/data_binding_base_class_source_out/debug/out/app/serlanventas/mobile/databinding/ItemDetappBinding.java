// Generated by view binder compiler. Do not edit!
package app.serlanventas.mobile.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import app.serlanventas.mobile.R;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;

public final class ItemDetappBinding implements ViewBinding {
  @NonNull
  private final LinearLayout rootView;

  @NonNull
  public final TextView textViewId;

  @NonNull
  public final TextView textViewPeso;

  @NonNull
  public final TextView textViewPollos;

  @NonNull
  public final TextView textViewTipo;

  @NonNull
  public final TextView textViewjabas;

  private ItemDetappBinding(@NonNull LinearLayout rootView, @NonNull TextView textViewId,
      @NonNull TextView textViewPeso, @NonNull TextView textViewPollos,
      @NonNull TextView textViewTipo, @NonNull TextView textViewjabas) {
    this.rootView = rootView;
    this.textViewId = textViewId;
    this.textViewPeso = textViewPeso;
    this.textViewPollos = textViewPollos;
    this.textViewTipo = textViewTipo;
    this.textViewjabas = textViewjabas;
  }

  @Override
  @NonNull
  public LinearLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static ItemDetappBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static ItemDetappBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.item_detapp, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static ItemDetappBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.textViewId;
      TextView textViewId = ViewBindings.findChildViewById(rootView, id);
      if (textViewId == null) {
        break missingId;
      }

      id = R.id.textViewPeso;
      TextView textViewPeso = ViewBindings.findChildViewById(rootView, id);
      if (textViewPeso == null) {
        break missingId;
      }

      id = R.id.textViewPollos;
      TextView textViewPollos = ViewBindings.findChildViewById(rootView, id);
      if (textViewPollos == null) {
        break missingId;
      }

      id = R.id.textViewTipo;
      TextView textViewTipo = ViewBindings.findChildViewById(rootView, id);
      if (textViewTipo == null) {
        break missingId;
      }

      id = R.id.textViewjabas;
      TextView textViewjabas = ViewBindings.findChildViewById(rootView, id);
      if (textViewjabas == null) {
        break missingId;
      }

      return new ItemDetappBinding((LinearLayout) rootView, textViewId, textViewPeso,
          textViewPollos, textViewTipo, textViewjabas);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}
