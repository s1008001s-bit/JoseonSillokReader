package kr.co.personal.sillokreader

import android.annotation.SuppressLint
import android.app.*
import android.content.*
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.EngineInfo
import android.view.Gravity
import android.webkit.*
import android.widget.*
import java.util.*

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var web: WebView
    private lateinit var tts: TextToSpeech
    private lateinit var title: TextView
    private val p by lazy { getSharedPreferences("sillok", MODE_PRIVATE) }
    private val home = "https://sillok.history.go.kr/search/inspectionList.do"
    private var rate=1.0f; private var zoom=100; private var unit="문장"; private var engine=""
    private var queue = mutableListOf<String>(); private var index=0
    override fun onCreate(b: Bundle?) { super.onCreate(b); rate=p.getFloat("rate",1f); zoom=p.getInt("zoom",100); engine=p.getString("engine","") ?: ""; tts=TextToSpeech(this,this); buildUi(); web.loadUrl(p.getString("url",home) ?: home) }

    @SuppressLint("SetJavaScriptEnabled") private fun buildUi(){
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(0xFFF7F0E4.toInt())}
        val bar=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(2,2,2,2);setBackgroundColor(0xFF292621.toInt())}
        fun b(s:String, f:()->Unit)=Button(this).apply{text=s;setOnClickListener{f()};minWidth=44;setTextColor(0xFFFFF8E8.toInt());setBackgroundColor(0x00292621)}
        bar.addView(b("‹"){if(web.canGoBack())web.goBack()});bar.addView(b("›"){if(web.canGoForward())web.goForward()});bar.addView(b("⌂"){web.loadUrl(home)})
        title=TextView(this).apply{text="  조선왕조실록";textSize=16f;setTextColor(0xFFFFF8E8.toInt());gravity=Gravity.CENTER_VERTICAL;maxLines=1}
        bar.addView(title,LinearLayout.LayoutParams(0,54,1f));bar.addView(b("▶"){readPage()});bar.addView(b("■"){stop()});bar.addView(b("☰"){menu()})
        web=WebView(this).apply{settings.javaScriptEnabled=true;settings.domStorageEnabled=true;settings.textZoom=zoom;addJavascriptInterface(object { @android.webkit.JavascriptInterface fun text(v:String){runOnUiThread{prepare(v)}} }, "AndroidReader");settings.builtInZoomControls=true;settings.displayZoomControls=false;settings.allowFileAccess=false;webChromeClient=WebChromeClient();webViewClient=object:WebViewClient(){override fun onPageFinished(v:WebView,u:String){p.edit().putString("url",u).apply();title.text="  ${v.title ?: "조선왕조실록"}";style()}}}
        root.addView(bar);root.addView(web,LinearLayout.LayoutParams(-1,0,1f));setContentView(root)
    }
    private fun style(){web.evaluateJavascript("document.body.style.lineHeight='1.85';document.body.style.fontSize='18px';",null)}
    private fun readPage(){
        val js="""(function(){let a=[...document.querySelectorAll('p,li,article,div')].map(x=>x.innerText?.trim()).filter(x=>x&&x.length>20);a=a.sort((x,y)=>y.length-x.length).slice(0,8);let t=a.join('\\n\\n');if(!t)t=document.body.innerText;window.AndroidReader.text(t)})();"""
        web.evaluateJavascript(js,null)
    }
    private fun prepare(text:String){
        val cleaned=text.replace(Regex("\\s+")," ").trim(); if(cleaned.isEmpty()){toast("읽을 본문이 없습니다.");return}
        queue=when(unit){"문단"->text.split(Regex("\\n\\s*\\n"));"줄"->text.split(Regex("\\n"));else->text.split(Regex("(?<=[.!?。！？])\\s+"))}.map{it.trim()}.filter{it.isNotEmpty()}.toMutableList();index=0; speakCurrent()
    }
    private fun speakCurrent(){if(index>=queue.size){stop();return};tts.stop();tts.setOnUtteranceProgressListener(object:android.speech.tts.UtteranceProgressListener(){override fun onStart(id:String?){ };override fun onDone(id:String?){runOnUiThread{index++;speakCurrent()}};override fun onError(id:String?){runOnUiThread{stop()}}});tts.setSpeechRate(rate);tts.speak(queue[index],TextToSpeech.QUEUE_FLUSH,null,"sillok-${index}")
    }
    private fun speak(text:String){tts.stop();tts.setSpeechRate(rate);tts.speak(text,TextToSpeech.QUEUE_FLUSH,null,"sillok")}
    private fun stop(){tts.stop()}
    private fun menu(){
        AlertDialog.Builder(this).setTitle("조선왕조실록").setItems(arrayOf("전자책 읽기 방식","음성 엔진 선택","읽기 속도","글자 크기","책갈피","공식 실록 사이트","음성 설정")){_,i->when(i){0->unitDialog();1->engineDialog();2->rateDialog();3->zoomDialog();4->marks();5->web.loadUrl(home);6->startActivity(Intent("com.android.settings.TTS_SETTINGS"))}}.show()
    }
    private fun unitDialog(){val xs=arrayOf("문장","문단","줄");val cur=xs.indexOf(unit).coerceAtLeast(0);AlertDialog.Builder(this).setTitle("읽기 단위").setSingleChoiceItems(xs,cur){d,w->unit=xs[w];p.edit().putString("unit",unit).apply();d.dismiss()}.show()}
    private fun engineDialog(){val es=tts.engines;val names=es.map{it.label}.toTypedArray();val ids=es.map{it.name};val cur=ids.indexOf(engine);AlertDialog.Builder(this).setTitle("음성/AI 엔진 선택").setMessage("휴대폰에 TTS 엔진으로 등록된 음성만 표시됩니다. AI 음성이 TTS 엔진으로 제공되는 기기라면 여기에 나타납니다.").setSingleChoiceItems(names,cur){d,w->engine=ids[w];p.edit().putString("engine",engine).apply();tts.shutdown();tts=TextToSpeech(this,this,engine);d.dismiss()}.show()}
    private fun rateDialog(){val s=SeekBar(this).apply{max=250;progress=((rate-.5f)*100).toInt().coerceIn(0,250)};AlertDialog.Builder(this).setTitle("읽기 속도").setView(s).setPositiveButton("적용"){_,_->rate=.5f+s.progress/100f;p.edit().putFloat("rate",rate).apply()}.setNegativeButton("취소",null).show()}
    private fun zoomDialog(){val s=SeekBar(this).apply{max=80;progress=(zoom-80).coerceIn(0,80)};AlertDialog.Builder(this).setTitle("글자 크기").setView(s).setPositiveButton("적용"){_,_->zoom=80+s.progress;p.edit().putInt("zoom",zoom).apply();web.settings.textZoom=zoom}.show()}
    private fun marks(){val set=p.getStringSet("marks",emptySet())!!.toList();if(set.isEmpty()){toast("책갈피가 없습니다.");return};AlertDialog.Builder(this).setTitle("책갈피").setItems(set.map{it.substringBefore('|')}.toTypedArray()){_,w->web.loadUrl(set[w].substringAfter('|'))}.show()}
    private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
    override fun onInit(status:Int){if(status==TextToSpeech.SUCCESS){if(engine.isNotBlank()) tts.setEngineByPackageName(engine);tts.language=Locale.KOREAN;tts.setSpeechRate(rate)}}
    override fun onDestroy(){tts.stop();tts.shutdown();web.destroy();super.onDestroy()}
}

// Small adapter so the source remains compatible with Android's callback API.
private class UtteranceProgressListenerAdapter(private val delegate:android.speech.tts.UtteranceProgressListener):android.speech.tts.UtteranceProgressListener(){override fun onStart(id:String?){delegate.onStart(id)};override fun onDone(id:String?){delegate.onDone(id)};override fun onError(id:String?){delegate.onError(id)}}
