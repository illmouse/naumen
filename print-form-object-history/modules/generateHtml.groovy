def generateTimeline(subject,user) {
  def events = utils.find("${api.metainfo.getMetaClass(subject).toString().split('\\$')[0]}__Evt", ['parent' : subject])
  def comments = utils.comments(subject)
  def files = utils.files(subject)
  def objects = []

  comments.each {
    objects << [
      'type' : '🗨 Добавлен комментарий',
      'eventDate' : utils.formatters.strToDateTime(modules.itsm365.getDateTimeInUserTimeZone(it.creationDate, user)).format('YYYY-MM-dd HH:mm'),
      'event' : "<strong>${it.author?.title ?: ''}</strong><br>${api.string.htmlToText(it?.text ?: '')}"
    ]
  }

  files.each {
    objects << [
      'type' : '🖿 Добавлен файл',
      'eventDate' : utils.formatters.strToDateTime(modules.itsm365.getDateTimeInUserTimeZone(it.creationDate, user)).format('YYYY-MM-dd HH:mm'),
      'event' : "${it.author?.title ? '<strong>' + it.author?.title + '</strong>': '<strong>Суперпользователь</strong>'} <br> ${it?.title ?: 'Без названия'}"
    ]
  }

  events.each {
    def event
    if (it.responsibleChanged) {
      def resp = ''
      resp += it.responsibleEmployee ? it.responsibleEmployee.title : 'Без ответственного'
      resp += it.responsibleTeam ? "/${it.responsibleTeam.title}" : '/Без команды'
      def newResp = ''
      newResp += it.newResponsibleEmployee ? it.newResponsibleEmployee.title : 'Без ответственного'
      newResp += it.newResponsibleTeam ? "/${it.newResponsibleTeam.title}" : '/Без командыо'
      event = "${resp}   ➤   ${newResp}"
    } else if (it.stateChanged) {
      event = "${api.metainfo.getStateTitle(api.metainfo.getMetaClass(subject).toString().split('\\$')[0], it.stateCode.toString()) ?: 'Отсутствует'}   ➤   ${api.metainfo.getStateTitle(api.metainfo.getMetaClass(subject).toString().split('\\$')[0], it.newStateCode.toString()) ?: 'Отсутствует'}"
    }
    objects << [
      'type' : "${it.responsibleChanged ? '🗣 Смена ответственного' : '⇆ Смена статуса'}",
      'eventDate' : utils.formatters.strToDateTime(modules.itsm365.getDateTimeInUserTimeZone(it.eventDate, user)).format('YYYY-MM-dd HH:mm'),
      'event' : event
    ]
  }

  objects = objects.sort { it.eventDate }
  def html = ''
  for (obj in objects) {
    html += "<tr><td class='eventDate'>${obj.eventDate}</td><td class='objType'>${obj.type}</td><td class='event'>${obj.event}</td></tr>"
  }
  return html
}


def generateDescription(subject, user) {
  def map = api.metainfo.getMetaClass(subject).getAttributeGroup('DlyaPechatnoiFormy').attributes

  def result = ''
  if (map) {
    map.each {
      def title = it.title
      def value
      switch ((subject."${it.code}").getClass().getSimpleName()) {
        case 'NullObject':
          value = ''
        break
        case 'String':
          value = "${it.code}" == 'state' ? api.metainfo.getStateTitle(subject) : api.string.htmlToText(subject."${it.code}" ?: '')
        break
        case 'Boolean':
          value = subject."${it.code}" == true ? 'Да' : 'Нет'
        break
        case 'ScriptDtObject':
          value = subject."${it.code}" ? subject."${it.code}".title : ''
        break
        case 'ScriptDtOList':
          if (subject."${it.code}") {
            subject."${it.code}".each {
              value += it?.title ? " ${it.title}  " : ''
            }
          }
        break
        case 'ScriptDate':
        	value = subject."${it.code}" ? utils.formatters.strToDateTime(modules.itsm365.getDateTimeInUserTimeZone(subject."${it.code}", user)).format('YYYY-MM-dd HH:mm') : subject."${it.code}"
        break
        case 'AggregateContainerWrapper':
        	value = subject."${it.code}" ? subject."${it.code}".title : ''
        break
        default:
          value = subject."${it.code}" ? subject."${it.code}".toString() : ''
      }
      result += value != '' ? "<tr><td class='descrTitle'>${title}</td><td class='descrValue'>${value}</td></tr>" : ''
    }
  }
  return result
}

def generateQR(subject) {
    def text = api.web.open(subject)
    def width = 100
    def height = 100
    def bytes = api.barcode.getBarCode(text, "QR_CODE", "PNG", width, height)
    return "<img src=\"data:image/png;base64,${bytes.encodeBase64().toString()}\" width=\"${width}\" height=\"${height}\">"
}