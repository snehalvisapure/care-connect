// ============================================================
// payment.js
// Opens the Razorpay checkout popup for a PENDING money donation and
// reports the result back to the page.
//
// Needs, loaded BEFORE this file (in this order):
//   <script src="../../js/api.js"></script>               (provides Api.payments)
//   <script src="https://checkout.razorpay.com/v1/checkout.js"></script>
//   <script src="../../js/payment.js"></script>
//
// Usage:
//   CareConnectPay.start({
//     donationId: 12,
//     description: "Donation to Sunshine old age home",
//     prefill: { name: "Asha", email: "asha@example.com", contact: "9999999999" },
//     onSuccess: (result) => { ... },   // payment verified, donation completed
//     onFailure: (err)    => { ... },   // an error to show the user (a failed attempt can be retried
//                                       //   inside the popup, so this may be called more than once)
//     onCancel:  ()       => { ... }    // the flow ended WITHOUT payment (popup closed / could not start)
//   });
//
// The amount is never sent from here: the server reads it from the donations table.
// ============================================================

(function (global) {
  "use strict";

  async function start(opts) {
    var onSuccess = opts.onSuccess || function () {};
    var onFailure = opts.onFailure || function () {};
    var onCancel = opts.onCancel || function () {};
    var finished = false;

    // Ends the flow without a successful payment.
    function giveUp(err) {
      if (finished) { return; }
      finished = true;
      if (err) { onFailure(err); }
      onCancel();
    }

    try {
      if (!global.Razorpay) {
        throw new Error("Payment window could not be loaded. Check your internet connection.");
      }

      var order = await Api.payments.createOrder("DONATION", opts.donationId);

      var rzp = new global.Razorpay({
        key: order.keyId,
        amount: order.amount,
        currency: order.currency,
        order_id: order.orderId,
        name: "CareConnect",
        description: opts.description || "Donation",
        prefill: opts.prefill || {},
        theme: { color: "#1F4D3D" },
        handler: async function (resp) {
          finished = true; // the popup completed, so closing it must not count as a cancel
          try {
            var result = await Api.payments.verify({
              razorpay_order_id: resp.razorpay_order_id,
              razorpay_payment_id: resp.razorpay_payment_id,
              razorpay_signature: resp.razorpay_signature
            });
            onSuccess(result);
          } catch (e) {
            var msg = (e && e.message) ? e.message : "Verification failed.";
            onFailure(new Error(msg + " If money was deducted, contact support quoting order " + order.orderId + "."));
            onCancel();
          }
        },
        modal: {
          ondismiss: function () { giveUp(null); }
        }
      });

      rzp.on("payment.failed", function (r) {
        var msg = (r && r.error && r.error.description) || "Payment failed.";
        onFailure(new Error(msg));
      });

      rzp.open();
    } catch (e) {
      giveUp(e);
    }
  }

  global.CareConnectPay = { start: start };
})(window);
