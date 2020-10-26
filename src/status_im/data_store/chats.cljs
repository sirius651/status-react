(ns status-im.data-store.chats
  (:require [clojure.set :as clojure.set]
            [status-im.data-store.messages :as messages]
            [status-im.ethereum.json-rpc :as json-rpc]
            [status-im.constants :as constants]
            [status-im.utils.fx :as fx]
            [taoensso.timbre :as log]))

(defn rpc->type [{:keys [chatType name] :as chat}]
  (cond
    (= constants/public-chat-type chatType) (assoc chat
                                                   :chat-name (str "#" name)
                                                   :public? true
                                                   :group-chat true)
    (= constants/community-chat-type chatType) (assoc chat
                                                      :group-chat true
                                                      :chat-name name)
    (= constants/private-group-chat-type chatType) (assoc chat
                                                          :chat-name name
                                                          :public? false
                                                          :group-chat true)
    :else (assoc chat :public? false :group-chat false)))

(defn- marshal-members [{:keys [admins contacts members-joined chat-type] :as chat}]
  (cond-> chat
    (= chat-type constants/private-group-chat-type)
    (assoc :members (map #(hash-map :id %
                                    :admin (boolean (admins %))
                                    :joined (boolean (members-joined %))) contacts))
    :always
    (dissoc :admins :contacts :members-joined)))

(defn- unmarshal-members [{:keys [members chatType] :as chat}]
  (cond
    (= constants/public-chat-type chatType) (assoc chat
                                                   :contacts #{}
                                                   :admins #{}
                                                   :members-joined #{})
    (= constants/private-group-chat-type chatType) (merge chat
                                                          (reduce (fn [acc member]
                                                                    (cond-> acc
                                                                      (:admin member)
                                                                      (update :admins conj (:id member))
                                                                      (:joined member)
                                                                      (update :members-joined conj (:id member))
                                                                      :always
                                                                      (update :contacts conj (:id member))))
                                                                  {:admins #{}
                                                                   :members-joined #{}
                                                                   :contacts #{}}
                                                                  members))
    :else
    (assoc chat
           :contacts #{(:id chat)}
           :admins #{}
           :members-joined #{})))

(defn- ->rpc [chat]
  (-> chat
      marshal-members
      (update :last-message messages/->rpc)
      (clojure.set/rename-keys {:chat-id :id
                                :membership-update-events :membershipUpdateEvents
                                :chat-type :chatType
                                :unviewed-messages-count :unviewedMessagesCount
                                :last-message :lastMessage
                                :deleted-at-clock-value :deletedAtClockValue
                                :is-active :active
                                :last-clock-value :lastClockValue})
      (dissoc :public? :group-chat :messages
              :might-have-join-time-messages?
              :loaded-unviewed-messages-ids
              :contacts :admins :members-joined)))

(defn <-rpc [chat]
  (-> chat
      rpc->type
      unmarshal-members
      (clojure.set/rename-keys {:id :chat-id
                                :organisationId :community-id
                                :membershipUpdateEvents :membership-update-events
                                :deletedAtClockValue :deleted-at-clock-value
                                :chatType :chat-type
                                :unviewedMessagesCount :unviewed-messages-count
                                :lastMessage :last-message
                                :active :is-active
                                :lastClockValue :last-clock-value
                                :invitationAdmin :invitation-admin})
      (update :last-message #(when % (messages/<-rpc %)))
      (dissoc :members)))

(fx/defn save-chat [cofx {:keys [chat-id] :as chat} on-success]
  {::json-rpc/call [{:method (json-rpc/call-ext-method "saveChat")
                     :params [(->rpc chat)]
                     :on-success #(do
                                    (log/debug "saved chat" chat-id "successfuly")
                                    (when on-success (on-success)))
                     :on-failure #(log/error "failed to save chat" chat-id %)}]})

(fx/defn fetch-chats-rpc [cofx {:keys [on-success]}]
  {::json-rpc/call [{:method (json-rpc/call-ext-method "chats")
                     :params []
                     :on-success #(on-success (map <-rpc %))
                     :on-failure #(log/error "failed to fetch chats" 0 -1 %)}]})
