package depollsoft.lib.kotlin.ui

fun android.app.Dialog.safeDismiss() {
    if (this.isShowing) {
        this.dismiss()
    }
}