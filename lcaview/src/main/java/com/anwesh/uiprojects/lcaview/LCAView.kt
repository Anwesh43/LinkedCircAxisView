package com.anwesh.uiprojects.lcaview

/**
 * Created by anweshmishra on 23/07/18.
 */

import android.view.View
import android.view.MotionEvent
import android.content.Context
import android.graphics.Paint
import android.graphics.Canvas
import android.graphics.Color

val nodes : Int = 5

fun Canvas.drawLCANode(i : Int, scale : Float, paint : Paint) {
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    val gap : Float = (w / 2) / nodes
    val size : Float = gap / 5
    save()
    translate((i * gap + gap - size) * scale, 0f)
    drawCircle(0f, 0f, size/2, paint)
    restore()
}

fun Canvas.drawInAxis(cb : () -> Unit) {
    save()
    translate(width.toFloat()/2, height.toFloat()/2)
    for (i in 0..3) {
        save()
        rotate(90f * i)
        cb()
        restore()
    }
    restore()
}

class LCAView(ctx : Context) : View(ctx) {

    private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas : Canvas) {

    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {

            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var prevScale : Float = 0f, var dir : Float = 0f) {

        fun update(stopcb : (Float) -> Unit) {
            scale += 0.1f * dir
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                stopcb(prevScale)
            }
        }

        fun startUpdating(startcb : () -> Unit) {
            if (dir == 0f) {
                dir = 1 - 2 * prevScale
                startcb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {

        fun animate(cb : () -> Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(50)
                    view.invalidate()
                } catch(ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    data class LCANode(var i : Int, val state : State = State()) {

        var prev : LCANode? = null

        var next : LCANode? = null

        fun addNeighbor() {
            if (i < nodes - 1) {
                next = LCANode(i + 1)
                next?.prev = this
            }
        }

        init {
            addNeighbor()
        }

        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawLCANode(i, state.scale, paint)
        }

        fun update(stopcb : (Int, Float) -> Unit) {
            state.update {
                stopcb(i, it)
            }
        }

        fun startUpdating(startcb : () -> Unit) {
            state.startUpdating(startcb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : LCANode {
            var curr : LCANode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    data class LinkedCircAxis(var i : Int) {

        private var curr : LCANode = LCANode(0)

        private var dir : Int = 1

        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawInAxis {
                curr.draw(canvas, paint)
            }
        }

        fun update(stopcb : (Int, Float) -> Unit) {
            curr.update {i, scale ->
                curr = curr.getNext(dir) {
                    dir *= -1
                }
                stopcb(i, scale)
            }
        }

        fun startUpdating(startcb : () -> Unit) {
            curr.startUpdating(startcb)
        }
    }

    data class Renderer(var view : LCAView) {

        private val animator : Animator = Animator(view)

        private val lca : LinkedCircAxis = LinkedCircAxis(0)

        fun render(canvas : Canvas, paint : Paint) {
            canvas.drawColor(Color.parseColor("#212121"))
            lca.draw(canvas, paint)
            animator.animate {
                lca.update {i, scale ->
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            lca.startUpdating {
                animator.start()
            }
        }
    }
}