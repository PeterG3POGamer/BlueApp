// Generated by view binder compiler. Do not edit!
package app.serlanventas.mobile.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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

public final class ToastCustomBinding implements ViewBinding {
  @NonNull
  private final LinearLayout rootView;

  @NonNull
  public final ImageView toastIcon;

  @NonNull
  public final TextView toastMessage;

  private ToastCustomBinding(@NonNull LinearLayout rootView, @NonNull ImageView toastIcon,
      @NonNull TextView toastMessage) {
    this.rootView = rootView;
    this.toastIcon = toastIcon;
    this.toastMessage = toastMessage;
  }

  @Override
  @NonNull
  public LinearLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static ToastCustomBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static ToastCustomBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.toast_custom, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static ToastCustomBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.toast_icon;
      ImageView toastIcon = ViewBindings.findChildViewById(rootView, id);
      if (toastIcon == null) {
        break missingId;
      }

      id = R.id.toast_message;
      TextView toastMessage = ViewBindings.findChildViewById(rootView, id);
      if (toastMessage == null) {
        break missingId;
      }

      return new ToastCustomBinding((LinearLayout) rootView, toastIcon, toastMessage);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}
