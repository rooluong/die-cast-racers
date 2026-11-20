package com.diecastracers.game

import android.app.Activity
import android.content.Intent
import android.graphics.*
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

class MainActivity : Activity() {
    private lateinit var game: RacerView
    private val cameraRequest = 410
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        game = RacerView(this) { action ->
            if (action == "camera") {
                try {
                    val i = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
                    startActivityForResult(i, cameraRequest)
                } catch (_: Exception) { game.showMessage("Camera app not available") }
            }
        }
        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
             View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
             View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
        setContentView(game)
    }
    @Deprecated("Legacy camera intent result")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == cameraRequest && resultCode == RESULT_OK) {
            val bitmap = data?.extras?.get("data") as? Bitmap
            if (bitmap != null) game.addScannedCar(bitmap)
            else game.showMessage("Photo not received. Try again.")
        }
    }
}

private data class ToyCar(val name: String, val color: Int, val kind: Int, val stars: Int = 0)
private enum class Screen { HOME, GARAGE, RACE, WIN, PARENT }

class RacerView(ctx: android.content.Context, val action: (String) -> Unit) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 45)
    private var screen = Screen.HOME
    private val cars = mutableListOf(
        ToyCar("Ruby Rocket", Color.rgb(245,65,75), 0),
        ToyCar("Blue Bolt", Color.rgb(45,135,245), 1),
        ToyCar("Sunny Truck", Color.rgb(255,190,35), 2),
        ToyCar("Green Flash", Color.rgb(45,195,115), 3)
    )
    private var selected = 0
    private var track = 0
    private var progress = 0f
    private var steering = 0f
    private var raceMode = 0
    private var message = ""
    private var messageUntil = 0L
    private var parentHoldAt = 0L
    private var parentUnlocked = false
    private var stars = 0
    private var coins = 0
    private var lastTick = System.currentTimeMillis()
    private var raceStart = 0L
    private var boosting = false
    private val tracks = listOf("Rainbow Run","Candy Canyon","Rocket Loop","Cloud Jumper","Bubble Bay",
        "Dino Dash","Sunset Spiral","Star Speedway","Jellybean Jump","Supernova Circuit")
    private val sky = intArrayOf(0xFF72D8FF.toInt(),0xFFFFB5D5.toInt(),0xFF9BE7C3.toInt(),0xFFFFD27D.toInt())
    private fun color(c:Int)=c
    private fun rounded(c:Canvas, x:Float,y:Float,w:Float,h:Float,r:Float,col:Int) {
        p.color=col; p.style=Paint.Style.FILL; c.drawRoundRect(x,y,x+w,y+h,r,r,p)
    }
    private fun text(c:Canvas,s:String,x:Float,y:Float,size:Float,col:Int=Color.WHITE,bold:Boolean=true,center:Boolean=false) {
        p.color=col; p.textSize=size; p.typeface=if(bold) Typeface.create("sans-serif-rounded",Typeface.BOLD) else Typeface.create("sans-serif",Typeface.NORMAL)
        p.textAlign=if(center) Paint.Align.CENTER else Paint.Align.LEFT
        c.drawText(s,x,y,p)
    }
    private fun circle(c:Canvas,x:Float,y:Float,r:Float,col:Int) { p.color=col; c.drawCircle(x,y,r,p) }
    private fun button(c:Canvas,label:String,x:Float,y:Float,w:Float,h:Float,col:Int=0xFFFFC928.toInt()) {
        rounded(c,x,y,w,h,h/2,0x55301B45); rounded(c,x,y-5,w,h,h/2,col)
        text(c,label,x+w/2,y+h*0.66f-5,h*0.48f,0xFF25305A.toInt(),true,true)
    }
    override fun onDraw(c:Canvas) {
        super.onDraw(c)
        val w=width.toFloat(); val h=height.toFloat()
        val bg=when(screen){Screen.RACE->sky[track%sky.size];Screen.WIN->0xFF6DD6B5.toInt();else->0xFF76D9FF.toInt()}
        c.drawColor(bg)
        // soft cloud puffs
        for(i in 0..4){ val x=(w*(i+0.12f)/5f); val y=h*(0.13f+(i%2)*0.10f); circle(c,x,y,22f,0x88FFFFFF.toInt()); circle(c,x+24,y+4,28f,0x88FFFFFF.toInt()); circle(c,x+48,y,20f,0x88FFFFFF.toInt()) }
        when(screen) {
            Screen.HOME -> drawHome(c,w,h)
            Screen.GARAGE -> drawGarage(c,w,h)
            Screen.RACE -> drawRace(c,w,h)
            Screen.WIN -> drawWin(c,w,h)
            Screen.PARENT -> drawParent(c,w,h)
        }
        if(message.isNotEmpty() && System.currentTimeMillis()<messageUntil) {
            rounded(c,w*.28f,h*.06f,w*.44f,h*.13f,24f,0xEE26345F.toInt())
            text(c,message,w*.5f,h*.145f,h*.045f,Color.WHITE,true,true)
        }
        if(screen==Screen.RACE) postInvalidateDelayed(32)
    }
    private fun drawHome(c:Canvas,w:Float,h:Float) {
        text(c,"DIE-CAST",w*.5f,h*.22f,h*.12f,0xFF26345F.toInt(),true,true)
        text(c,"RACERS!",w*.5f,h*.34f,h*.12f,0xFFFF5A70.toInt(),true,true)
        drawCar(c,w*.5f,h*.55f,1.25f,cars[selected].color,cars[selected].kind)
        text(c,"Little Cars. Big Adventures!",w*.5f,h*.77f,h*.045f,0xFF26345F.toInt(),true,true)
        button(c,"PLAY!",w*.36f,h*.82f,w*.28f,h*.13f)
        button(c,"GARAGE",w*.04f,h*.83f,w*.20f,h*.12f,0xFF9CEBBD.toInt())
        button(c,"PARENTS",w*.76f,h*.83f,w*.20f,h*.12f,0xFFFFFFFF.toInt())
    }
    private fun drawGarage(c:Canvas,w:Float,h:Float) {
        text(c,"MY GARAGE",w*.5f,h*.13f,h*.075f,0xFF26345F.toInt(),true,true)
        text(c,"Swipe or tap arrows to choose a car",w*.5f,h*.22f,h*.035f,0xFF26345F.toInt(),false,true)
        drawCar(c,w*.5f,h*.52f,1.55f,cars[selected].color,cars[selected].kind)
        text(c,cars[selected].name,w*.5f,h*.72f,h*.06f,0xFF26345F.toInt(),true,true)
        button(c,"◀",w*.12f,h*.43f,w*.13f,h*.15f,0xFFFFFFFF.toInt())
        button(c,"▶",w*.75f,h*.43f,w*.13f,h*.15f,0xFFFFFFFF.toInt())
        button(c,"SCAN NEW CAR",w*.35f,h*.79f,w*.30f,h*.13f,0xFFFFD45B.toInt())
        button(c,"BACK",w*.03f,h*.04f,w*.15f,h*.12f,0xFFFFFFFF.toInt())
    }
    private fun drawRace(c:Canvas,w:Float,h:Float) {
        // toy track with broad lane, dashed center
        val path=Path().apply { moveTo(w*.08f,h*.74f); cubicTo(w*.20f,h*.34f,w*.36f,h*.34f,w*.48f,h*.62f); cubicTo(w*.62f,h*.94f,w*.78f,h*.24f,w*.94f,h*.55f) }
        p.color=0xFFEF5B9A.toInt(); p.style=Paint.Style.STROKE; p.strokeWidth=h*.28f; p.strokeCap=Paint.Cap.ROUND; c.drawPath(path,p)
        p.color=0xFFFFE7F2.toInt(); p.strokeWidth=h*.018f; p.pathEffect=DashPathEffect(floatArrayOf(22f,18f),0f); c.drawPath(path,p); p.pathEffect=null; p.style=Paint.Style.FILL
        // boost pads
        rounded(c,w*.66f,h*.49f,w*.10f,h*.07f,10f,0xFFFFD33D.toInt())
        text(c,"⚡",w*.71f,h*.545f,h*.055f,0xFF25305A.toInt(),true,true)
        val x=w*(.12f+.72f*progress)+steering*w*.05f
        val y=h*(.64f-.12f*sin(progress*PI*2).toFloat())
        drawCar(c,x,y,.85f,cars[selected].color,cars[selected].kind)
        text(c,tracks[track],w*.05f,h*.11f,h*.05f,0xFF26345F.toInt(),true)
        rounded(c,w*.37f,h*.045f,w*.27f,h*.055f,18f,0x88FFFFFF.toInt())
        rounded(c,w*.38f,h*.052f,w*.25f*progress,h*.041f,15f,0xFFFFC928.toInt())
        button(c,"GO!",w*.79f,h*.76f,w*.17f,h*.19f,0xFFFFD33D.toInt())
        button(c,"Ⅱ",w*.04f,h*.79f,w*.12f,h*.14f,0xFFFFFFFF.toInt())
        text(c,"⭐ ${stars}     🪙 ${coins}",w*.72f,h*.12f,h*.04f,0xFF26345F.toInt(),true)
    }
    private fun drawWin(c:Canvas,w:Float,h:Float) {
        text(c,"YOU DID IT!",w*.5f,h*.20f,h*.12f,0xFF26345F.toInt(),true,true)
        drawCar(c,w*.5f,h*.48f,1.3f,cars[selected].color,cars[selected].kind)
        text(c,"⭐  ⭐  ⭐",w*.5f,h*.69f,h*.09f,0xFFFFB800.toInt(),true,true)
        text(c,"Amazing driving!",w*.5f,h*.78f,h*.05f,0xFF26345F.toInt(),true,true)
        button(c,"RACE AGAIN",w*.28f,h*.84f,w*.24f,h*.12f)
        button(c,"MORE TRACKS",w*.55f,h*.84f,w*.24f,h*.12f,0xFF9CEBBD.toInt())
    }
    private fun drawParent(c:Canvas,w:Float,h:Float) {
        text(c,"GROWN-UP CORNER",w*.5f,h*.19f,h*.075f,0xFF26345F.toInt(),true,true)
        text(c,"Hold the button for 3 seconds to unlock car scanning.",w*.5f,h*.39f,h*.04f,0xFF26345F.toInt(),true,true)
        button(c,"HOLD TO UNLOCK",w*.35f,h*.52f,w*.30f,h*.18f,0xFFFFD45B.toInt())
        button(c,"BACK",w*.04f,h*.82f,w*.16f,h*.12f,0xFFFFFFFF.toInt())
        text(c,"No ads • No chat • Offline play",w*.5f,h*.75f,h*.035f,0xFF26345F.toInt(),false,true)
    }
    private fun drawCar(c:Canvas,x:Float,y:Float,scale:Float,col:Int,kind:Int) {
        val s=scale
        // shadow, wheels, glossy toy body
        p.color=0x44303B55; c.drawOval(x-78*s,y+25*s,x+82*s,y+49*s,p)
        circle(c,x-45*s,y+20*s,20*s,0xFF26334A.toInt()); circle(c,x+48*s,y+20*s,20*s,0xFF26334A.toInt())
        circle(c,x-45*s,y+20*s,10*s,0xFFB9D8E8.toInt()); circle(c,x+48*s,y+20*s,10*s,0xFFB9D8E8.toInt())
        val body=Path().apply { moveTo(x-88*s,y+10*s); quadTo(x-92*s,y-12*s,x-58*s,y-18*s); lineTo(x-26*s,y-48*s); quadTo(x+8*s,y-60*s,x+40*s,y-35*s); lineTo(x+78*s,y-19*s); quadTo(x+92*s,y-8*s,x+87*s,y+12*s); close() }
        p.color=col; c.drawPath(body,p)
        p.color=0x66FFFFFF; c.drawRoundRect(x-27*s,y-42*s,x+30*s,y-22*s,12*s,12*s,p)
        rounded(c,x-73*s,y-3*s,18*s,7*s,4f,0xFFFFF5C4.toInt())
        if(kind==2){ rounded(c,x-18*s,y-62*s,48*s,10*s,4f,0xFF37445D.toInt()) }
        else if(kind==3){ rounded(c,x+30*s,y-43*s,28*s,8*s,4f,0xFFFFFFFF.toInt()) }
        else if(kind==1){ rounded(c,x-65*s,y-26*s,28*s,9*s,4f,0xFFFFFFFF.toInt()) }
        p.color=0x55FFFFFF; c.drawArc(x-65*s,y-10*s,x+64*s,y+20*s,190f,130f,false,p)
    }
    private fun startRace() { screen=Screen.RACE; progress=0f; steering=0f; raceStart=System.currentTimeMillis(); tone.startTone(ToneGenerator.TONE_PROP_BEEP,120); invalidate() }
    private fun showWin() { screen=Screen.WIN; stars+=3; coins+=10; tone.startTone(ToneGenerator.TONE_PROP_ACK,300); invalidate() }
    fun showMessage(s:String) { message=s; messageUntil=System.currentTimeMillis()+2600; invalidate() }
    fun addScannedCar(bitmap:Bitmap) {
        val small=Bitmap.createScaledBitmap(bitmap,24,24,true)
        var rr=0L; var gg=0L; var bb=0L
        for(y in 0 until 24) for(x in 0 until 24) { val px=small.getPixel(x,y); rr+=Color.red(px); gg+=Color.green(px); bb+=Color.blue(px) }
        val col=Color.rgb((rr/576).toInt(),(gg/576).toInt(),(bb/576).toInt())
        val kind=if(bitmap.width>bitmap.height) 2 else 0
        cars.add(ToyCar("My Custom Car ${cars.size-3}",col,kind))
        selected=cars.lastIndex; screen=Screen.GARAGE
        showMessage("New car unlocked!")
    }
    override fun onTouchEvent(e:MotionEvent):Boolean {
        val w=width.toFloat(); val h=height.toFloat()
        when(e.action) {
            MotionEvent.ACTION_DOWN -> {
                if(screen==Screen.PARENT && e.y>h*.48f && e.y<h*.76f) { parentHoldAt=System.currentTimeMillis(); return true }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if(screen==Screen.PARENT && parentHoldAt>0) {
                    val held=System.currentTimeMillis()-parentHoldAt; parentHoldAt=0
                    if(held>=3000) { parentUnlocked=true; action("camera"); showMessage("Take a photo of a toy car") }
                    else showMessage("Keep holding for 3 seconds")
                    return true
                }
                when(screen) {
                    Screen.HOME -> when {
                        e.y>h*.80f && e.x>w*.34f && e.x<w*.67f -> startRace()
                        e.y>h*.80f && e.x<w*.27f -> screen=Screen.GARAGE
                        e.y>h*.80f && e.x>w*.72f -> screen=Screen.PARENT
                    }
                    Screen.GARAGE -> when {
                        e.y<h*.20f && e.x<w*.22f -> screen=Screen.HOME
                        e.y in h*.35f..h*.68f && e.x<w*.30f -> selected=(selected-1+cars.size)%cars.size
                        e.y in h*.35f..h*.68f && e.x>w*.70f -> selected=(selected+1)%cars.size
                        e.y>h*.75f && e.x in w*.30f..w*.70f -> screen=Screen.PARENT
                    }
                    Screen.RACE -> when {
                        e.y>h*.72f && e.x>w*.75f -> { progress=(progress+.20f).coerceAtMost(1f); if(progress>=1f) showWin() else tone.startTone(ToneGenerator.TONE_PROP_BEEP,70) }
                        e.y>h*.75f && e.x<w*.20f -> screen=Screen.HOME
                        else -> { steering=if(e.x<w*.5f) -.7f else .7f; progress=(progress+.06f).coerceAtMost(1f); if(progress>=1f) showWin() }
                    }
                    Screen.WIN -> if(e.y>h*.80f) {
                        if(e.x>w*.52f) track=(track+1)%tracks.size
                        startRace()
                    }
                    Screen.PARENT -> if(e.y>h*.78f) screen=if(parentUnlocked) Screen.GARAGE else Screen.HOME
                }
                invalidate(); return true
            }
        }
        return true
    }
}
