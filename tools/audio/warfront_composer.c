#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>
#include <math.h>

/* Warfront procedural score renderer.
   Uses the FluidSynth shared library and a General MIDI SoundFont.
   No Python or online music generator is used for composition/rendering. */
typedef struct _fluid_settings_t fluid_settings_t;
typedef struct _fluid_synth_t fluid_synth_t;
extern fluid_settings_t *new_fluid_settings(void);
extern void delete_fluid_settings(fluid_settings_t*);
extern int fluid_settings_setnum(fluid_settings_t*, const char*, double);
extern int fluid_settings_setint(fluid_settings_t*, const char*, int);
extern int fluid_settings_setstr(fluid_settings_t*, const char*, const char*);
extern fluid_synth_t *new_fluid_synth(fluid_settings_t*);
extern int delete_fluid_synth(fluid_synth_t*);
extern int fluid_synth_sfload(fluid_synth_t*, const char*, int);
extern int fluid_synth_program_select(fluid_synth_t*, int, int, int, int);
extern int fluid_synth_noteon(fluid_synth_t*, int, int, int);
extern int fluid_synth_noteoff(fluid_synth_t*, int, int);
extern int fluid_synth_write_float(fluid_synth_t*, int, void*, int, int, void*, int, int);

#define SR 44100
#define BPM 126.0
#define BEAT (60.0/BPM)
#define BARS 48
#define BEATS (BARS*4)
#define TOTAL_SAMPLES ((int)(BEATS*BEAT*SR))
#define MAX_EVENTS 30000

typedef struct { int sample, note, vel, on, channel; } Event;
typedef struct { Event e[MAX_EVENTS]; int n; } Track;
static const char *sfpath="/usr/share/sounds/sf2/TimGM6mb.sf2";
static int global_sfid=0;

static void add(Track *t, double beat, int note, int vel, int on, int ch) {
  if(t->n>=MAX_EVENTS) return;
  int s=(int)llround(beat*BEAT*SR); if(s<0) s=0;
  t->e[t->n++]=(Event){s,note,vel,on,ch};
}
static int cmp_event(const void *a,const void *b){const Event*x=a,*y=b;return x->sample-y->sample;}
static void note(Track*t,double b,double d,int n,int v,int ch){add(t,b,n,v,1,ch);add(t,b+d,n,0,0,ch);}
static void chord(Track*t,double b,double d,const int *ns,int count,int v,int ch){for(int i=0;i<count;i++)note(t,b,d,ns[i],v,ch);}
static fluid_synth_t *make_synth(int reverb,int chorus){
  fluid_settings_t *s=new_fluid_settings();
  fluid_settings_setnum(s,"synth.sample-rate",SR);
  fluid_settings_setnum(s,"synth.gain",0.62);
  fluid_settings_setint(s,"synth.reverb.active",reverb);
  fluid_settings_setint(s,"synth.chorus.active",chorus);
  fluid_settings_setint(s,"synth.polyphony",256);
  fluid_settings_setint(s,"synth.cpu-cores",2);
  fluid_synth_t *sy=new_fluid_synth(s);
  int id=fluid_synth_sfload(sy,sfpath,1);
  if(id<0){fprintf(stderr,"Could not load SoundFont %s\n",sfpath);exit(2);}
  global_sfid=id;
  delete_fluid_settings(s); return sy;
}
static void write_wav_header(FILE*f,int frames){
  uint32_t data=frames*4, riff=36+data; uint16_t fmt=1,ch=2,bits=16,align=4; uint32_t rate=SR,br=SR*4; uint32_t extra=16;
  fwrite("RIFF",1,4,f);fwrite(&riff,4,1,f);fwrite("WAVEfmt ",1,8,f);fwrite(&extra,4,1,f);fwrite(&fmt,2,1,f);fwrite(&ch,2,1,f);fwrite(&rate,4,1,f);fwrite(&br,4,1,f);fwrite(&align,2,1,f);fwrite(&bits,2,1,f);fwrite("data",1,4,f);fwrite(&data,4,1,f);
}
static void render_track(const char*path,Track*t,int program,int percussion){
  qsort(t->e,t->n,sizeof(Event),cmp_event);
  fluid_synth_t*sy=make_synth(1,1);
  if(!percussion) { fluid_synth_program_select(sy,0,global_sfid,0,program); if(program==48) fluid_synth_program_select(sy,1,global_sfid,0,42); }
  FILE*f=fopen(path,"wb"); if(!f){perror(path);exit(3);} write_wav_header(f,TOTAL_SAMPLES);
  float l[1024],r[1024]; int16_t out[2048]; int cursor=0,ei=0;
  while(cursor<TOTAL_SAMPLES){
    int next=(ei<t->n?t->e[ei].sample:TOTAL_SAMPLES); if(next>TOTAL_SAMPLES)next=TOTAL_SAMPLES;
    while(cursor<next){int n=next-cursor;if(n>1024)n=1024;
      fluid_synth_write_float(sy,n,l,0,1,r,0,1);
      for(int i=0;i<n;i++){double a=l[i],b=r[i]; if(a>1)a=1;if(a< -1)a=-1;if(b>1)b=1;if(b< -1)b=-1;out[2*i]=(int16_t)(a*32767);out[2*i+1]=(int16_t)(b*32767);} fwrite(out,sizeof(int16_t),n*2,f);cursor+=n;
    }
    while(ei<t->n && t->e[ei].sample<=cursor){Event e=t->e[ei++];if(e.on)fluid_synth_noteon(sy,e.channel,e.note,e.vel);else fluid_synth_noteoff(sy,e.channel,e.note);}
  }
  fclose(f);delete_fluid_synth(sy);
}
static void build_foundation(Track*t){
  const int roots[4]={38,34,41,36};
  const int triads[4][4]={{50,53,57,62},{46,50,53,58},{53,57,60,65},{48,52,55,60}};
  for(int bar=0;bar<BARS;bar++){
    int p=(bar/2)%4; double b=bar*4.0; int bass=roots[p];
    note(t,b,3.7,bass,bar<4?78:96,1);
    note(t,b+2,1.7,bass+12,bar<4?55:78,1);
    for(int k=0;k<8;k++){
      double v=bar<4?0.0:(bar<20?0.45:(bar<36?0.8:1.0));
      if(v>0) note(t,b+k*0.5,0.38,triads[p][k%3]-12,(int)(55+35*v),1);
    }
    if(bar>=2){int vel=bar<8?42:(bar<24?58:76);chord(t,b,3.8,triads[p],4,vel,0);}
  }
}
static void build_brass(Track*t){
  const int melody[16]={62,65,69,74,72,69,65,67,69,72,77,76,74,69,67,65};
  const double lens[16]={1,1,1.5,.5,1,1,1,1,1,1,1.5,.5,1,1,1,1};
  for(int bar=4;bar<BARS;bar+=2){
    double start=bar*4.0; int phrase=((bar-4)/8)%2;
    for(int i=0;i<8;i++){int idx=phrase?15-i:i;int vel=bar<12?70:(bar<28?92:112);note(t,start+i*0.5,lens[idx]*0.78,melody[idx]+(bar>=28?12:0),vel,0);}
    int low=(bar%8<4)?50:46;
    note(t,start,1.35,low,bar<16?66:100,0);
    note(t,start+2,1.2,low+7,bar<16?58:92,0);
  }
}
static void build_drums(Track*t){
  for(int bar=0;bar<BARS;bar++){
    double b=bar*4.0; int intensity=bar<4?0:(bar<12?1:(bar<28?2:3));
    if(!intensity)continue;
    int kick=(intensity==1?78:(intensity==2?100:118));
    for(int beat=0;beat<4;beat++){
      if(beat==0||beat==2||intensity>=2)note(t,b+beat,0.18,36,kick,9);
      if(beat==1||beat==3)note(t,b+beat,0.14,38,intensity==1?48:82,9);
      if(intensity>=2)note(t,b+beat+0.5,0.08,42,44,9);
    }
    if(bar%4==3)note(t,b+3.5,0.25,49,intensity==3?112:72,9);
    if(intensity==3 && bar%2==1){note(t,b+3,0.18,45,82,9);note(t,b+3.5,0.18,47,90,9);}
  }
}
static void build_battle(Track*t){
  for(int bar=12;bar<BARS;bar++){
    double b=bar*4.0; int v=bar<28?55:(bar<40?78:104);
    int root[4]={50,46,53,48}; int r=root[(bar/2)%4];
    for(int k=0;k<4;k++){if(bar>=20 || k==0 || k==2)note(t,b+k,0.48,r+12,v,0);}
    if(bar>=28){note(t,b,0.9,r+19,v,0);note(t,b+1.5,0.42,r+22,v-8,0);note(t,b+2,0.9,r+19,v,0);note(t,b+3.5,0.42,r+24,v-8,0);}
  }
}
int main(int argc,char**argv){
  const char*out=(argc>1?argv[1]:"./warfront-output");
  char p[512]; Track foundation={0},brass={0},drums={0},battle={0};
  build_foundation(&foundation);build_brass(&brass);build_drums(&drums);build_battle(&battle);
  snprintf(p,sizeof(p),"%s/01_foundation.wav",out);render_track(p,&foundation,48,0);
  snprintf(p,sizeof(p),"%s/02_brass_theme.wav",out);render_track(p,&brass,61,0);
  snprintf(p,sizeof(p),"%s/03_war_drums.wav",out);render_track(p,&drums,0,1);
  snprintf(p,sizeof(p),"%s/04_battle_drive.wav",out);render_track(p,&battle,60,0);
  fprintf(stderr,"Rendered %d bars at %.0f BPM (%d seconds).\n",BARS,BPM,(int)(BEATS*BEAT));
  return 0;
}