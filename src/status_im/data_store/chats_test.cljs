(ns status-im.data-store.chats-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [status-im.data-store.chats :as chats]))

(deftest ->to-rpc
  (let [chat {:public? false
              :group-chat true
              :color "color"
              :contacts #{"a" "b" "c" "d"}
              :last-clock-value 10
              :chat-type 3
              :admins #{"a" "b"}
              :members-joined #{"a" "c"}
              :name "name"
              :membership-update-events :events
              :unviewed-messages-count 2
              :is-active true
              :chat-id "chat-id"
              :loaded-unviewed-messages-ids []
              :timestamp 2}
        expected-chat {:id "chat-id"
                       :color "color"
                       :name "name"
                       :chatType 3
                       :lastMessage nil
                       :members #{{:id "a"
                                   :admin true
                                   :joined true}
                                  {:id "b"
                                   :admin true
                                   :joined false}
                                  {:id "c"
                                   :admin false
                                   :joined true}
                                  {:id "d"
                                   :admin false
                                   :joined false}}
                       :lastClockValue 10
                       :membershipUpdateEvents :events
                       :unviewedMessagesCount 2
                       :active true
                       :timestamp 2}]
    (testing "marshaling chat"
      (is (= expected-chat (-> (#'status-im.data-store.chats/->rpc chat)
                               (update :members #(into #{} %))))))))

(deftest normalize-chat-test
  (let [chat {:id "chat-id"
              :color "color"
              :name "name"
              :chatType 3
              :members [{:id "a"
                         :admin true
                         :joined true}
                        {:id "b"
                         :admin true
                         :joined false}
                        {:id "c"
                         :admin false
                         :joined true}
                        {:id "d"
                         :admin false
                         :joined false}]
              :lastClockValue 10
              :membershipUpdateEvents :events
              :unviewedMessagesCount 2
              :active true
              :timestamp 2}
        expected-chat {:public? false
                       :group-chat true
                       :color "color"
                       :chat-name "name"
                       :contacts #{"a" "b" "c" "d"}
                       :chat-type 3
                       :last-clock-value 10
                       :last-message nil
                       :admins #{"a" "b"}
                       :members-joined #{"a" "c"}
                       :name "name"
                       :membership-update-events :events
                       :unviewed-messages-count 2
                       :is-active true
                       :chat-id "chat-id"
                       :timestamp 2}]
    (testing "from-rpc"
      (is (= expected-chat (chats/<-rpc chat))))))
