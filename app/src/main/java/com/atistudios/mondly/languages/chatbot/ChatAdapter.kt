package com.atistudios.mondly.languages.chatbot

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.os.Handler
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.ConstraintSet.PARENT_ID
import androidx.core.view.ViewCompat
import androidx.core.view.ViewPropertyAnimatorListener
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.atistudios.mondly.languages.chatbot.ext.getScreenWidth
import com.atistudios.mondly.languages.chatbot.ext.play
import com.atistudios.mondly.languages.chatbot.ext.scaleAnimation
import com.bumptech.glide.Glide
import jp.wasabeef.recyclerview.animators.holder.AnimateViewHolder
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.adt_chat_bot_message.*
import kotlinx.android.synthetic.main.adt_chat_user_message.*


private const val ITEM_SLIDE_DURATION = 250L
private const val ALPHA_SLIDE_DURATION = 500L
private const val TEXT_SCALE_DURATION = 250L
private const val TEXT_SCALE_FACTOR = 1.02F
private const val DELAY_FOR_FIXING_OVERLAPPING_WITH_PROGRESS = 300L

internal class ChatAdapter(
    private val botMessageClickListener: ((message: String, factor: Float) -> Unit)?
) :
    ListAdapter<ChatMessage, BaseViewHolder>(
        object : DiffUtil.ItemCallback<ChatMessage>() {
            override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
                return oldItem.id == newItem.id
            }

            @SuppressLint("DiffUtilEquals")
            override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
                return oldItem == newItem
            }
        }) {

    enum class ItemViewType {
        BOT_MESSAGE_TYPE, USER_MESSAGE_TYPE, FOOTER_TYPE
    }

    private var enableSpeak = true

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when (ItemViewType.values()[viewType]) {
            ItemViewType.BOT_MESSAGE_TYPE -> BaseViewHolder.BotMessageViewHolder(
                layoutInflater.inflate(R.layout.adt_chat_bot_message, parent, false)
            ) { message, rate ->
                if (enableSpeak) {
                    botMessageClickListener?.invoke(message, rate)
                }
            }
            ItemViewType.USER_MESSAGE_TYPE -> BaseViewHolder.UserMessageViewHolder(
                layoutInflater.inflate(R.layout.adt_chat_user_message, parent, false)
            )
            ItemViewType.FOOTER_TYPE -> BaseViewHolder.FooterViewHolder(
                layoutInflater.inflate(R.layout.adt_chat_footer, parent, false)
            )
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ChatMessage.BotMessage -> ItemViewType.BOT_MESSAGE_TYPE.ordinal
            is ChatMessage.UserMessage -> ItemViewType.USER_MESSAGE_TYPE.ordinal
            is ChatMessage.Footer -> ItemViewType.FOOTER_TYPE.ordinal
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        when (holder) {
            is BaseViewHolder.BotMessageViewHolder -> {
                holder.bindView(getItem(position) as ChatMessage.BotMessage)
            }
            is BaseViewHolder.UserMessageViewHolder -> {
                holder.bindView(getItem(position) as ChatMessage.UserMessage)
            }
            is BaseViewHolder.FooterViewHolder -> holder.bindView(getItem(position) as ChatMessage.Footer)
        }
    }

    fun enableSpeak() {
        enableSpeak = true
    }

    fun disableSpeak() {
        enableSpeak = false
    }
}

internal sealed class BaseViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView),
    LayoutContainer {

    class BotMessageViewHolder(
        containerView: View,
        private val botMessageClickListener: (message: String, rate: Float) -> Unit?
    ) :
        BaseViewHolder(containerView), AnimateViewHolder {

        init {
            // ugly hack to limit max width of user message TextView
            text_message.maxWidth =
                (containerView.context as Activity).getScreenWidth() -
                        containerView.context.resources.getDimension(R.dimen.adt_chat_views_width).toInt()
        }

        override fun preAnimateAddImpl(holder: RecyclerView.ViewHolder) {
            holder.itemView.alpha = 0F
            img_constraint.translationX = -img_constraint.width.toFloat()
        }

        override fun preAnimateRemoveImpl(holder: RecyclerView.ViewHolder) {
            holder.itemView.alpha = 1F
            img_constraint.translationX = 0F
        }

        override fun animateAddImpl(holder: RecyclerView.ViewHolder, listener: ViewPropertyAnimatorListener?) {
            val translationAnimator = ObjectAnimator.ofFloat(
                img_constraint, "translationX",
                -img_constraint.width.toFloat(), 0F
            )
                .apply {
                    duration = ITEM_SLIDE_DURATION
                    interpolator = AccelerateInterpolator()
                }
            val alphaAnimator = ObjectAnimator.ofFloat(itemView, "alpha", 0F, 1F)
                .apply {
                    duration = ALPHA_SLIDE_DURATION
                }
            AnimatorSet()
                .apply {
                    play(alphaAnimator).before(translationAnimator)
                    addListener(object : Animator.AnimatorListener {
                        override fun onAnimationRepeat(animation: Animator?) {
                        }

                        override fun onAnimationEnd(animation: Animator?) {
                            listener?.onAnimationEnd(img_constraint)
                        }

                        override fun onAnimationCancel(animation: Animator?) {
                            listener?.onAnimationCancel(img_constraint)
                        }

                        override fun onAnimationStart(animation: Animator?) {
                            listener?.onAnimationStart(img_constraint)
                        }
                    })
                    start()
                }
        }

        override fun animateRemoveImpl(holder: RecyclerView.ViewHolder, listener: ViewPropertyAnimatorListener?) {}

        fun bindView(item: ChatMessage.BotMessage) {
            loader_bot_message.isInvisible = !item.isLoading
            Handler().postDelayed({
                TransitionManager.beginDelayedTransition(message_container)
                text_message.isVisible = !item.isLoading
                text_message.text = item.text
                text_message_translation.text = item.translation
                if (item.showTranslation) {
                    ConstraintSet()
                        .apply {
                            clone(message_container)
                            clear(R.id.text_message, ConstraintSet.BOTTOM)
                            applyTo(message_container)
                        }
                } else {
                    ConstraintSet()
                        .apply {
                            clone(message_container)
                            connect(R.id.text_message, ConstraintSet.BOTTOM, PARENT_ID, ConstraintSet.BOTTOM, 0)
                            applyTo(message_container)
                        }
                }
            }, DELAY_FOR_FIXING_OVERLAPPING_WITH_PROGRESS)
            container_message.setOnClickListener {
                onItemClicked(item.text)
            }
            bg_img_bot_speaker.setOnClickListener {
                onItemClicked(item.text)
            }
            img_bot_avatar.setImageResource(R.drawable.ic_emoji)
            text_message_translation.animate().scaleY(if (item.showTranslation && !item.isLoading) 1F else 0F)
        }

        private fun onItemClicked(text: String?) {
            img_bot_speaker.play {
                if (!text.isNullOrEmpty()) {
                    botMessageClickListener.invoke(text, it)
                }
                text_message.scaleAnimation(TEXT_SCALE_FACTOR, TEXT_SCALE_DURATION)
            }
        }
    }


    class UserMessageViewHolder(containerView: View) : BaseViewHolder(containerView), AnimateViewHolder {

        init {
            // ugly hack to limit max width of user message TextView
            text_user_message.maxWidth =
                (containerView.context as Activity).getScreenWidth() - containerView.context.resources.getDimension(R.dimen.adt_user_views_width).toInt()
        }

        override fun preAnimateAddImpl(holder: RecyclerView.ViewHolder) {
            holder.itemView.alpha = 0F
        }

        override fun preAnimateRemoveImpl(holder: RecyclerView.ViewHolder) {
            holder.itemView.alpha = 1F
        }

        override fun animateAddImpl(holder: RecyclerView.ViewHolder, listener: ViewPropertyAnimatorListener?) {
            ViewCompat.animate(holder.itemView)
                .alpha(1F)
                .setDuration(ALPHA_SLIDE_DURATION)
                .setListener(listener)
        }

        override fun animateRemoveImpl(holder: RecyclerView.ViewHolder, listener: ViewPropertyAnimatorListener?) {}

        fun bindView(item: ChatMessage.UserMessage) {
            img_message_icon.isVisible = item.icon != null && !item.isSpeaking
            if (item.icon != null && !item.isSpeaking) {
                Glide.with(containerView)
                    .load(item.icon)
                    .into(img_message_icon)
            } else {
                img_message_icon.setImageBitmap(null)
            }
            TransitionManager.beginDelayedTransition(user_message_container)
            loader_user_message.isVisible = item.isSpeaking
            text_user_message.isVisible = !item.isSpeaking
            text_user_message.text = item.text

            //todo remove this line and uncomment next one for loading the real avatar
            img_user_avatar.setImageResource(R.drawable.ic_avatar)
//            if (item.avatarUrl != null) {
//                Glide.with(containerView)
//                    .load(item.avatarUrl)
//                    .into(img_user_avatar)
//            } else {
//                img_user_avatar.setImageBitmap(null)
//            }
        }
    }

    class FooterViewHolder(containerView: View) : BaseViewHolder(containerView) {
        fun bindView(item: ChatMessage.Footer) {
            val lp = containerView.layoutParams.apply { height = item.height }
            containerView.layoutParams = lp
        }
    }
}