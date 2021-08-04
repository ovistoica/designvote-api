(ns designvote.payment.routes)

(defn routes
  []
  [""
   ["/create-customer"
    {:post {:summary "Create a new customer in Stripe"}}]
   ["/create-subscription"
    {:post {:summary "Add a new subscription for a customer"}}]])
