package xyz.jdynb.tv.event

import xyz.jdynb.tv.model.LiveChannelModel

interface Playable {

  fun play(channel: LiveChannelModel)

  fun resumeOrPause()
}